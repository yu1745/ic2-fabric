package ic2_120.content.uu

import ic2_120.Ic2_120
import ic2_120.config.UuReplicationDefaults
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import java.util.IdentityHashMap

/**
 * UU 复制成本运行时索引：服务器启动时构建 [UuGraph]，提供物品 → UU 成本(uB) 的查询。
 *
 * ## 数据源与优先级
 * 1. **显式白名单**（[UuReplicationDefaults]，230 条手工定价）：作为 [UuGraph] 的硬约束
 *    （locked），动态计算不可覆盖。查询时也优先返回白名单值。
 * 2. **动态计算**（[UuGraph] Bellman-Ford）：补全白名单未覆盖、但合成链能回溯到白名单物品的
 *    衍生物品（机器、工具、钚链、各种加工品）。
 *
 * ## 生命周期
 * - [register] 挂到 [ServerLifecycleEvents.SERVER_STARTED]，每次启动重建（耗时 ~50ms）
 * - 构建完成后调用 [onRebuilt] 回调（供 JEI 刷新等）
 * - 服务器停止时清空（避免持有已失效的 Item 引用）
 *
 * ## 单位
 * 内部 value（cobblestone=1.0）× 10 = uB（micro-bucket）。1 mB UU = 1_000_000 EU ⇒ 1 uB = 1000 EU。
 */
object UuCostIndex {

    private val logger = LoggerFactory.getLogger("ic2_120.uu")

    /** 构建完成后的回调（用于触发 JEI 配方刷新）*/
    private var onRebuilt: (() -> Unit)? = null

    /** 是否在构建后 dump 全量结果到日志（调试用，默认关）*/
    var dumpOnRebuild: Boolean = false

    /** 若非 null，构建后把全量消耗表写入此文件（调试用）*/
    var dumpPath: String? = null

    /** dump 上限：只显示成本 ≤ 此值（uB）的物品 */
    private const val MAX_UB_DUMP = 5_000_000L

    /** 构建是否完成（完成前所有查询返回 null，由调用方 fallback 到白名单）*/
    @Volatile
    private var ready: Boolean = false

    /** 动态算出的成本缓存：Item -> uB（仅含非白名单物品，避免重复查 UuGraph）*/
    private val dynamicCache: IdentityHashMap<net.minecraft.world.item.Item, Long> = IdentityHashMap()

    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            rebuild(server)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            dynamicCache.clear()
            ready = false
        }
    }

    /** 设置构建完成回调 */
    fun onRebuild(callback: () -> Unit) {
        onRebuilt = callback
    }

    private fun rebuild(server: MinecraftServer) {
        val t0 = System.nanoTime()
        UuGraph.clear()

        // 1. 注入白名单作为硬约束
        val (ok, missing) = UuScanValues.loadInto(UuGraph)
        if (missing > 0) {
            logger.debug("UU 白名单 {} 条未注册物品已跳过", missing)
        }

        // 2. 收集配方
        val recipeManager: RecipeManager = server.recipeManager
        val result = UuRecipeBridge.collect(recipeManager, server.registryManager)
        result.transformations.forEach { UuGraph.addTransformation(it) }

        // 3. 求解
        val iter = UuGraph.solve()
        val elapsed = (System.nanoTime() - t0) / 1_000_000

        // 4. 构建动态缓存（非白名单物品）
        dynamicCache.clear()
        val whitelistIds = UuReplicationDefaults.defaultWhitelist.keys
        var dynamicCount = 0
        for ((item, _) in UuGraph.allValues()) {
            if (UuGraph.value(item).isInfinite()) continue
            val id = BuiltInRegistries.ITEM.getId(item).toString()
            if (id in whitelistIds) continue  // 白名单物品走 Ic2Config，不缓存
            val ub = UuGraph.costUb(item)
            if (ub > 0) {
                dynamicCache[item] = ub
                dynamicCount++
            }
        }
        ready = true

        logger.info(
            "UU 成本索引构建完成: 白名单 {} 条, 动态 {} 条, 配方 {}, 迭代 {} 轮, 耗时 {}ms",
            ok, dynamicCount, result.transformations.size, iter, elapsed
        )

        if (dumpOnRebuild) dumpAll()
        dumpPath?.let { writeTableFile(it, server) }

        // 5. 通知 JEI 等刷新
        onRebuilt?.invoke()
    }

    /**
     * 查询物品的动态 UU 成本（uB）。仅返回动态算出的；白名单物品请走 [Ic2Config]。
     * @return 动态成本，或 null（未构建完成 / 该物品无路径）
     */
    fun dynamicCostUb(item: net.minecraft.world.item.Item): Long? =
        if (ready) dynamicCache[item] else null

    /** 是否已构建完成 */
    fun isReady(): Boolean = ready

    /** 所有动态算出的（itemId -> uB）*/
    fun allDynamic(): Map<String, Long> {
        if (!ready) return emptyMap()
        val out = LinkedHashMap<String, Long>()
        for ((item, ub) in dynamicCache) {
            out[BuiltInRegistries.ITEM.getId(item).toString()] = ub
        }
        return out
    }

    /** 调试用：dump 全量结果到日志 */
    private fun dumpAll() {
        logger.info("==================== UU Cost Dump ====================")
        val whitelistIds = UuReplicationDefaults.defaultWhitelist.keys

        // 白名单物品的实际值（应等于白名单定价）
        logger.info("---- 白名单定价（抽查）----")
        val spot = listOf(
            "minecraft:cobblestone", "minecraft:iron_ingot", "minecraft:diamond",
            "ic2_120:uranium_ore", "ic2_120:steel_ingot",
            "ic2_120:plutonium", "ic2_120:depleted_quad_mox_fuel_rod"
        )
        for (idStr in spot) {
            val item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(idStr))
            if (item === Items.AIR) continue
            val expected = UuReplicationDefaults.defaultWhitelist[idStr]
            val actual = UuGraph.costUb(item)
            val mark = if (expected != null && actual == expected.toLong()) "✓" else "?"
            logger.info("  $mark $idStr = $actual uB (白名单=${expected ?: "无"})")
        }

        // 动态新增，按成本升序
        logger.info("---- 动态算出的物品（≤ ${MAX_UB_DUMP} uB，按成本升序）----")
        val dynamic = allDynamic().entries
            .filter { it.value in 1..MAX_UB_DUMP }
            .sortedBy { it.value }
        for ((id, ub) in dynamic) {
            logger.info(String.format("  %-45s %12d uB", id, ub))
        }
        logger.info("(共 ${dynamic.size} 个)")

        // 未覆盖
        logger.info("---- IC2 物品中无成本的 ----")
        val inf = BuiltInRegistries.ITEM
            .filter { it !== Items.AIR && BuiltInRegistries.ITEM.getId(it).namespace == Ic2_120.MOD_ID }
            .filter { UuGraph.value(it).isInfinite() }
            .sortedBy { BuiltInRegistries.ITEM.getId(it).path }
        for (item in inf) logger.info("  ∞  ${BuiltInRegistries.ITEM.getId(item)}")
        logger.info("(共 ${inf.size} 个)")

        logger.info("==================== UU Cost Dump End ====================")
    }

    /** 把全量消耗表（白名单 + 动态，按成本升序）写入文件 */
    private fun writeTableFile(path: String, server: MinecraftServer) {
        try {
            loadZhLangFromServer(server)
            val whitelist = UuReplicationDefaults.defaultWhitelist
            val dynamic = allDynamic()
            // 合并：itemId -> uB
            val merged = LinkedHashMap<String, Long>()
            for ((id, ub) in whitelist) if (ub > 0) merged[id] = ub.toLong()
            for ((id, ub) in dynamic) if (id !in merged && ub > 0) merged[id] = ub

            val sorted = merged.entries.sortedBy { it.value }
            val finiteCount = sorted.size
            val infItems = BuiltInRegistries.ITEM
                .filter { it !== Items.AIR && UuGraph.value(it).isInfinite() }
                .sortedBy { BuiltInRegistries.ITEM.getId(it).toString() }

            java.io.File(path).bufferedWriter(Charsets.UTF_8).use { w ->
                w.write("# IC2 UU 复制消耗表\n")
                w.write("# 生成时间: ${java.time.Instant.now()}\n")
                w.write("# 单位: uB (micro-bucket), 1 uB = 1000 EU, 1 mB UU = 1_000_000 EU\n")
                w.write("# 来源: 白名单(手工定价) ${whitelist.size} 条 + 动态计算 ${dynamic.size} 条\n")
                w.write("# 合计可复制: $finiteCount, 无成本(∞): ${infItems.size}\n")
                w.write("\n")
                w.write(String.format("%-48s %-30s %12s %s\n", "# itemId", "显示名", "uB", "来源"))
                for ((id, ub) in sorted) {
                    val src = if (id in whitelist) "白名单" else "动态"
                    val name = displayName(id)
                    w.write(String.format("%-48s %-30s %12d %s\n", id, name, ub, src))
                }
                w.write("\n# ===== 无成本物品（∞，合成链未通到白名单叶节点）=====\n")
                for (item in infItems) {
                    val id = BuiltInRegistries.ITEM.getId(item).toString()
                    w.write(String.format("%-48s %s\n", "∞  $id", displayName(id)))
                }
            }
            logger.info("UU 消耗表已写入 {} (可复制 {}, 无成本 {})", path, finiteCount, infItems.size)
        } catch (e: Exception) {
            logger.error("写入 UU 消耗表失败: {}", path, e)
        }
    }

    /** 中文语言缓存（由 [loadZhLangFromServer] 填充）*/
    private var zhLangCache: Map<String, String> = emptyMap()

    /** 从 server ResourceManager 加载所有命名空间的 zh_cn.json（原版 + ic2 + ic2_120 + 附属）*/
    private fun loadZhLangFromServer(server: MinecraftServer) {
        val merged = HashMap<String, String>()
        val rm = server.resourceManager
        for (ns in listOf("minecraft", "ic2", "ic2_120")) {
            val id = ResourceLocation(ns, "lang/zh_cn.json")
            try {
                val res = rm.getResource(id)
                if (res.isPresent) {
                    res.get().inputStream.use { stream ->
                        val obj = com.fasterxml.jackson.databind.ObjectMapper().readValue(stream, Map::class.java)
                        @Suppress("UNCHECKED_CAST")
                        for ((k, v) in obj as Map<String, Any>) merged[k] = v.toString()
                    }
                }
            } catch (e: Exception) {
                logger.debug("加载 zh_cn 失败: {}", ns)
            }
        }
        zhLangCache = merged
        logger.info("中文语言缓存加载: ${merged.size} 条")
    }

    /** 获取物品的本地化显示名（强制中文）*/
    private fun displayName(itemId: String): String {
        val id = ResourceLocation.tryParse(itemId) ?: return "?"
        val item = BuiltInRegistries.ITEM.get(id)
        if (item === Items.AIR) return "?"
        return try {
            val key = item.translationKey
            zhLangCache[key] ?: item.getName().string
        } catch (e: Exception) {
            itemId.substringAfterLast(':')
        }
    }
}

package ic2_120.content.debug

import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity
import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity.FuelProfile
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.fluid.Fluid
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

/**
 * 临时调试：服务器启动后遍历所有已注册流体，复用半流质发电机的真实匹配逻辑
 * ([SemifluidGeneratorBlockEntity.isSupportedFuelFluid] / [SemifluidGeneratorBlockEntity.getFuelProfile])，
 * 把命中燃料的流体及其来源 mod 打到日志。
 *
 * 用于验证 jar 扫描无法覆盖的"代码运行时注册流体/标签"情况（如 GTCEu、TechReborn）。
 *
 * 测完后删除本文件，并移除 Ic2_120.onInitialize() 里的 SemifluidFuelDump.register() 调用。
 */
object SemifluidFuelDump {

    private val logger = LoggerFactory.getLogger("ic2_120.semifluid_dump")

    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            dump()
        }
    }

    private fun dump() {
        // 收集所有非空、非纯客户端占位的流体
        val all = Registries.FLUID.ids
            .mapNotNull { id ->
                val fluid = Registries.FLUID.get(id)
                // 跳过 EMPTY / 占位
                if (fluid == net.minecraft.fluid.Fluids.EMPTY) return@mapNotNull null
                id to fluid
            }

        // 复用发电机真实判定
        val supported = mutableListOf<Pair<Identifier, FuelProfile>>()
        val unsupportedButNamed = mutableListOf<Identifier>() // 名字像油/燃料但被判定为不支持

        for ((id, fluid) in all) {
            val profile = SemifluidGeneratorBlockEntity.getFuelProfile(fluid)
            if (profile != null) {
                supported.add(id to profile)
            } else if (looksLikeFuel(id)) {
                unsupportedButNamed.add(id)
            }
        }

        val sb = StringBuilder()
        sb.appendLine("==================== Semifluid Generator Fuel Dump ====================")
        sb.appendLine("Total registered fluids: ${all.size}")
        sb.appendLine()
        sb.appendLine(">>> SUPPORTED (would burn in semifluid generator): ${supported.size}")

        // 按来源 mod 分组
        val byMod = supported.groupBy { it.first.namespace }
        for ((mod, list) in byMod.toSortedMap()) {
            sb.appendLine("  [$mod]")
            for ((id, profile) in list.sortedBy { it.first.toString() }) {
                val kind = when (profile) {
                    // 通过引用比较——companion 内两个 profile 是单例
                    else -> describeProfile(profile)
                }
                sb.appendLine("    $id  ->  $kind")
            }
        }

        sb.appendLine()
        sb.appendLine(">>> Looks like fuel but REJECTED (id suggests oil/fuel/biofuel/etc., but tag didn't match): ${unsupportedButNamed.size}")
        for (id in unsupportedButNamed.sortedBy { it.toString() }) {
            sb.appendLine("    $id")
        }
        sb.appendLine("==================== End Dump ====================")

        // 一次 info 输出（多行）
        for (line in sb.toString().split('\n')) {
            logger.info(line)
        }
    }

    /** 启发式：fluid id 看起来像油/燃料（用于找出"被漏掉"的候选） */
    private fun looksLikeFuel(id: Identifier): Boolean {
        val path = id.path.lowercase()
        return path in setOf("oil", "crude_oil", "petroleum", "fuel", "biofuel", "diesel",
            "gasoline", "ethanol", "biodiesel", "creosote", "plantoil", "plant_oil",
            "seed_oil", "seedoil", "sap", "latex", "resin", "kerosene", "naphtha",
            "lubricant", "methanol", "vegetable_oil", "canola_oil", "sunflower_oil",
            "olive_oil", "peanut_oil", "turpentine", "tar", "bitumen", "biomass")
            || path.contains("crude_oil")
            || path.contains("diesel")
            || path.contains("gasoline")
            || path.contains("ethanol")
            || path.contains("biofuel")
            || path.contains("biodiesel")
            || path.contains("plant_oil")
            || path.contains("plantoil")
            || path.contains("seed_oil")
            || path.endsWith("_oil")
            || path.endsWith("oil")
            || path.endsWith("_fuel")
    }

    private fun describeProfile(p: FuelProfile): String {
        // BIOFUEL_PROFILE = 32000/16 ; CREOSOTE_PROFILE = 3200/8
        return when {
            p.euPerBucket == 32_000L && p.euPerTick == 16L -> "BIOFUEL (32000 EU/bucket, 16 EU/t)"
            p.euPerBucket == 3_200L && p.euPerTick == 8L -> "CREOSOTE (3200 EU/bucket, 8 EU/t)"
            else -> "UNKNOWN profile(eu/bucket=${p.euPerBucket}, eu/t=${p.euPerTick})"
        }
    }
}

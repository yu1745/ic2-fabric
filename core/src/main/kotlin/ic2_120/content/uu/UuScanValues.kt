package ic2_120.content.uu

import ic2_120.config.UuReplicationDefaults
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/**
 * UU 叶节点 / 覆盖值加载器。
 *
 * ## 数据源
 * 直接复用 [ic2_120.config.UuReplicationDefaults.defaultWhitelist]——它是手工平衡过的
 * 完整定价表（226 条，单位 uB），**完全覆盖** IC2 exp 的 `uu_scan_values.ini`（91 条）
 * 且范围更广，因此不再使用 exp 的 ini。
 *
 * 这些显式定价作为 [UuGraph] 的：
 * - **叶节点**：合成链的起点（如 cobblestone、iron_ore）
 * - **覆盖值**：衍生物品（如 iron_ingot、oak_planks）也显式定价，会**绝对压制动摇计算结果**
 *   （因为 [UuGraph.solve] 只在 `perItem < current - ε` 时更新，显式值一旦注入就不会被算高）
 *
 * 动态计算只负责补全 whitelist 未覆盖、但合成链能回溯到已定价物品的衍生物品
 * （如 plutonium、uranium_238、mox、各种粉尘副产物）。
 *
 * ## 单位换算
 * [UuReplicationDefaults] 用 uB（micro-bucket），[UuGraph] 内部 value 满足 `uB = value × 10`。
 * 故注入时 `value = uB / 10`。
 */
object UuScanValues {

    private val logger = LoggerFactory.getLogger("ic2_120.uu.scan")

    /**
     * 把所有显式定价注入 [UuGraph] 作为初始值。
     * @return (成功数, 未注册物品数)
     */
    fun loadInto(graph: UuGraph): Pair<Int, Int> {
        var ok = 0
        var missing = 0
        for ((idStr, ub) in UuReplicationDefaults.defaultWhitelist) {
            val item = resolveItem(idStr)
            if (item == null) {
                missing++
                logger.debug("白名单物品未注册，跳过: {}", idStr)
                continue
            }
            graph.setInitial(item, ub.toDouble() / 10.0)
            ok++
        }
        return ok to missing
    }

    /**
     * 查询某物品是否有显式定价（用于区分「动态算出的」vs「白名单定的」）。
     */
    fun explicitCostUb(itemId: String): Int? =
        UuReplicationDefaults.defaultWhitelist[itemId]?.takeIf { it > 0 }

    private fun resolveItem(idStr: String): Item? {
        val core = idStr.substringBefore('@')
        val id = Identifier.tryParse(core) ?: return null
        val item = BuiltInRegistries.ITEM.getValue(id)
        return if (item === Items.AIR) null else item
    }
}

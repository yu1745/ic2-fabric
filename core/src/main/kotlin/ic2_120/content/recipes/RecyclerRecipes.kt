package ic2_120.content.recipes

import ic2_120.config.Ic2Config
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries

/**
 * ================================================================================
 * ⚠️ 回收机配方系统 - 不迁移到原生配方系统
 * ================================================================================
 *
 * 原因：回收机的特殊机制不适合使用 Minecraft 原生配方系统
 *
 * 1. **全局适用性**：回收机可以处理 Minecraft 中几乎所有物品，而不是特定输入→输出的配方
 *    - 任何不在黑名单中的物品都有概率被回收成废料
 *    - 这是一个"万能回收"机制，而非有限配方列表
 *
 * 2. **黑名单机制**：使用排除法而非包含法
 *    - 配置文件维护黑名单（recycler.blacklist）
 *    - 新物品默认可回收，除非明确加入黑名单
 *    - 这与原生配方系统的"白名单"理念相反
 *
 * 3. **概率性输出**：
 *    - 回收成功概率不是 100%，而是基于物品类型的概率
 *    - 概率逻辑在机器运行时动态计算，不适合静态配方定义
 *
 * 4. **配方数量问题**：
 *    - Minecraft 有数千种物品，如果为每个物品创建配方 JSON，会产生大量冗余文件
 *    - 维护成本极高，且每次游戏更新都可能增加新物品
 *
 * 5. **设计理念**：
 *    - 回收机的核心价值就是"什么都能回收"
 *    - 这是 IC2 的经典设计，保持代码实现更符合其设计初衷
 *
 * 因此，RecyclerRecipes 保持当前的代码实现（黑名单 + 概率计算），不迁移到原生配方系统。
 *
 * @see ic2_120.content.block.machines.RecyclerBlockEntity
 * ================================================================================
 */
/**
 * 回收机配方。
 * 使用排除法：大多数物品均可回收，仅少数不允许。
 */
object RecyclerRecipes {

    /** 内置黑名单（配置为空时兜底） */
    private val FALLBACK_BLOCKED_ITEMS: Set<Item> = setOf(
        Registries.ITEM.get(net.minecraft.util.Identifier("minecraft", "stick"))
    )

    /**
     * 检查物品是否可以被回收。
     * 排除法：仅当物品不在 [BLOCKED_ITEMS] 中且非空时方可回收。
     */
    fun canRecycle(input: ItemStack): Boolean {
        if (input.isEmpty) return false
        val item = input.item
        val configured = Ic2Config.current.recycler.blacklist
            .asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        if (configured.isEmpty()) {
            return item !in FALLBACK_BLOCKED_ITEMS
        }

        val itemId = Registries.ITEM.getId(item).toString().lowercase()
        return itemId !in configured
    }
}

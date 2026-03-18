package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries

/**
 * 回收机配方。
 * 使用排除法：大多数物品均可回收，仅少数不允许。
 */
object RecyclerRecipes {

    /** 不允许回收的物品 ID 集合 */
    private val BLOCKED_ITEMS: Set<Item> = setOf(
        Registries.ITEM.get(net.minecraft.util.Identifier("minecraft", "stick"))
    )

    /**
     * 检查物品是否可以被回收。
     * 排除法：仅当物品不在 [BLOCKED_ITEMS] 中且非空时方可回收。
     */
    fun canRecycle(input: ItemStack): Boolean {
        if (input.isEmpty) return false
        return input.item !in BLOCKED_ITEMS
    }
}

package ic2_120.content.upgrade

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * 机器输入合法性的轻量查询接口（P1 修正版核心）。
 *
 * 背景：PullingUpgradeComponent 试探候选物品时调用 `Container.canPlaceItem(slot, stack)`
 * 需要为每个候选构造 ItemStack（variant.toStack(1) 分配）并走完整的多态验证链。
 * 对配方类机器（电炉/研磨机/压缩机等），验证的实质是「该 item 是否存在匹配配方」，
 * 这可以由 RecipeInputLookupCache 以 (RecipeType, Item) 布尔缓存作答，零分配。
 *
 * 实现约定：
 *  - 返回 false 表示该 item 一定不被输入槽接受（快速否定）；
 *  - 返回 true 表示「值得走完整验证」——实现方可返回保守 true，
 *    完整语义仍由 isValid(slot, stack) 把关（pulling 在真正插入前仍会调一次 isValid）。
 *  - 该接口只做候选过滤，不改变 isValid 的最终裁决。
 */
interface IValidInputItem {

    /**
     * 快速判断 item 是否可能被该机器的输入槽接受。
     * 默认实现返回 true（保守放行，等价于无此接口）。
     */
    fun isValidInputItem(item: Item): Boolean = true

    /** 便捷入口：空物品直接否定。 */
    fun isValidInputItem(stack: ItemStack): Boolean =
        !stack.isEmpty && isValidInputItem(stack.item)
}

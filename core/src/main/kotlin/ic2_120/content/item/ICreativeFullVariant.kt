package ic2_120.content.item

import net.minecraft.item.ItemStack

/**
 * 物品实现此接口后，扫描器会在创造模式物品栏中自动追加一个「满变体」[ItemStack]。
 *
 * 用于储电盒（满电）、喷气背包（满燃料）、泡沫喷枪/建筑泡沫背包（满流体）等
 * 需要预设状态的创造模式变体。
 */
interface ICreativeFullVariant {
    fun createFullVariant(): ItemStack
}

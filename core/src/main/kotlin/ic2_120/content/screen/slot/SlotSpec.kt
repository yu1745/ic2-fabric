package ic2_120.content.screen.slot

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

/**
 * 槽位规则声明：
 * - 是否允许放入
 * - 是否允许取出
 * - 最大堆叠数量
 */
data class SlotSpec(
    val maxItemCount: Int = 64,
    val canInsert: (ItemStack) -> Boolean = { true },
    val canTake: (PlayerEntity) -> Boolean = { true }
)

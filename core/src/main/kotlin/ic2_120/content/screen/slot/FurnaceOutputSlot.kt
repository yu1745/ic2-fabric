package ic2_120.content.screen.slot

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

/**
 * 熔炉输出槽：玩家手动取物品时触发回调（用于掉落经验球）。
 * 继承 PredicateSlot 以复用 SlotSpec 规则，额外在 onTakeItem 中执行回调。
 */
class FurnaceOutputSlot(
    inventory: Inventory,
    index: Int,
    x: Int,
    y: Int,
    spec: SlotSpec,
    private val onTake: (PlayerEntity) -> Unit
) : PredicateSlot(inventory, index, x, y, spec) {

    override fun onTakeItem(player: PlayerEntity, stack: ItemStack) {
        onTake(player)
        super.onTakeItem(player, stack)
    }
}

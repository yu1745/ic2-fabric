package ic2_120.content.screen.slot

import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

/**
 * 熔炉输出槽：玩家手动取物品时触发回调（用于掉落经验球）。
 * 继承 PredicateSlot 以复用 SlotSpec 规则，额外在 onTakeItem 中执行回调。
 */
class FurnaceOutputSlot(
    inventory: Container,
    index: Int,
    x: Int,
    y: Int,
    spec: SlotSpec,
    private val onTake: (Player) -> Unit
) : PredicateSlot(inventory, index, x, y, spec) {

    override fun onTakeItem(player: Player, stack: ItemStack) {
        onTake(player)
        super.onTakeItem(player, stack)
    }
}

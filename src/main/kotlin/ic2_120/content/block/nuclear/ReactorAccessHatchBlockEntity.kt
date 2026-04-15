package ic2_120.content.block.nuclear

import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 反应堆访问接口方块实体。
 * Inventory 直接委托到对应的中心核反应堆，一切操作都是对中心的操作。
 */
@ModBlockEntity(block = ReactorAccessHatchBlock::class)
class ReactorAccessHatchBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory {

    constructor(pos: BlockPos, state: BlockState) : this(
        ReactorAccessHatchBlockEntity::class.type(),
        pos,
        state
    )

    fun getCentralReactorPublic(): NuclearReactorBlockEntity? = getCentralReactor()

    private fun getCentralReactor(): NuclearReactorBlockEntity? {
        val w = world ?: return null
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val be = w.getBlockEntity(pos.add(dx, dy, dz))
                    if (be is NuclearReactorBlockEntity && be.isPositionInStructure(pos)) return be
                }
            }
        }
        return null
    }

    // ========== Inventory 委托到中心反应堆 ==========
    override fun size(): Int = getCentralReactor()?.size() ?: 0
    override fun getStack(slot: Int): ItemStack = getCentralReactor()?.getStack(slot) ?: ItemStack.EMPTY
    override fun setStack(slot: Int, stack: ItemStack) = getCentralReactor()?.setStack(slot, stack) ?: Unit
    override fun removeStack(slot: Int, amount: Int): ItemStack =
        getCentralReactor()?.removeStack(slot, amount) ?: ItemStack.EMPTY
    override fun removeStack(slot: Int): ItemStack = getCentralReactor()?.removeStack(slot) ?: ItemStack.EMPTY
    override fun clear() = getCentralReactor()?.clear() ?: Unit
    override fun isEmpty(): Boolean = getCentralReactor()?.isEmpty() ?: true
    override fun markDirty() {
        getCentralReactor()?.markDirty()
        super.markDirty()
    }
    override fun canPlayerUse(player: PlayerEntity): Boolean =
        getCentralReactor()?.canPlayerUse(player) ?: false

    companion object {
        fun tick(world: World, pos: BlockPos, state: BlockState, be: ReactorAccessHatchBlockEntity) {
            // 不需要额外的 tick 逻辑，所有逻辑都由中心反应堆处理
        }
    }
}

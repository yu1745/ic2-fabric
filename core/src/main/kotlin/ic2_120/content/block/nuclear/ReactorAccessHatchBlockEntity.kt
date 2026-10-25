package ic2_120.content.block.nuclear

import ic2_120.compat.LegacyBlockEntity26

import ic2_120.compat.pos
import ic2_120.compat.world

import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * 反应堆访问接口方块实体。
 * Container 直接委托到对应的中心核反应堆，一切操作都是对中心的操作。
 */
@ModBlockEntity(block = ReactorAccessHatchBlock::class)
class ReactorAccessHatchBlockEntity(
    type: net.minecraft.world.level.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : LegacyBlockEntity26(type, pos, state), Container {

    constructor(pos: BlockPos, state: BlockState) : this(
        ReactorAccessHatchBlockEntity::class.type(),
        pos,
        state
    )

    fun getCentralReactorPublic(): NuclearReactorBlockEntity? = getCentralReactor()

    override fun getMaxStackSize(): Int = 1

    private fun getCentralReactor(): NuclearReactorBlockEntity? {
        val w = world ?: return null
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val be = w.getBlockEntity(pos.offset(dx, dy, dz))
                    if (be is NuclearReactorBlockEntity && be.isPositionInStructure(pos)) return be
                }
            }
        }
        return null
    }

    // ========== Container 委托到中心反应堆 ==========
    override fun getContainerSize(): Int = getCentralReactor()?.getContainerSize() ?: 0
    override fun getItem(slot: Int): ItemStack = getCentralReactor()?.getItem(slot) ?: ItemStack.EMPTY
    override fun setItem(slot: Int, stack: ItemStack) = getCentralReactor()?.setItem(slot, stack) ?: Unit
    override fun removeItem(slot: Int, amount: Int): ItemStack =
        getCentralReactor()?.removeItem(slot, amount) ?: ItemStack.EMPTY
    override fun removeItemNoUpdate(slot: Int): ItemStack = getCentralReactor()?.removeItemNoUpdate(slot) ?: ItemStack.EMPTY
    override fun clearContent() = getCentralReactor()?.clearContent() ?: Unit
    override fun isEmpty(): Boolean = getCentralReactor()?.isEmpty() ?: true
    override fun setChanged() {
        getCentralReactor()?.setChanged()
        super.setChanged()
    }
    override fun stillValid(player: Player): Boolean =
        getCentralReactor()?.stillValid(player) ?: false

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean =
        getCentralReactor()?.canPlaceItem(slot, stack) ?: true

    companion object {
        fun tick(world: Level, pos: BlockPos, state: BlockState, be: ReactorAccessHatchBlockEntity) {
            // 不需要额外的 tick 逻辑，所有逻辑都由中心反应堆处理
        }
    }
}

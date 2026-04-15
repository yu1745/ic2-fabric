package ic2_120.content.block.nuclear

import ic2_120.content.block.ITieredMachine
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage

/**
 * 核反应仓方块实体。
 * 与核反应堆相邻时，共享反应堆的能量实例；Inventory 直接委托到中心核反应堆，一切操作都是对中心的操作。
 * 实现了 ITieredMachine 接口，能量等级为 5。
 */
@ModBlockEntity(block = ReactorChamberBlock::class)
class ReactorChamberBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ITieredMachine {

    override val tier: Int = CHAMBER_TIER

    constructor(pos: BlockPos, state: BlockState) : this(
        ReactorChamberBlockEntity::class.type(),
        pos,
        state
    )

    fun findAdjacentReactorPublic(): NuclearReactorBlockEntity? = findAdjacentReactor()

    private fun findAdjacentReactor(): NuclearReactorBlockEntity? {
        val world = world ?: return null
        for (dir in Direction.values()) {
            val neighborPos = pos.offset(dir)
            val neighborState = world.getBlockState(neighborPos)
            if (neighborState.block is NuclearReactorBlock) {
                val be = world.getBlockEntity(neighborPos) as? NuclearReactorBlockEntity
                if (be != null) return be
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun getEnergyStorage(side: Direction?): EnergyStorage? {
        val reactor = findAdjacentReactor() ?: return null
        return reactor.sync.getSideStorage(side)
    }

    // ========== Inventory 委托到中心反应堆 ==========
    override fun size(): Int = findAdjacentReactor()?.size() ?: 0
    override fun getStack(slot: Int): ItemStack = findAdjacentReactor()?.getStack(slot) ?: ItemStack.EMPTY
    override fun setStack(slot: Int, stack: ItemStack) = findAdjacentReactor()?.setStack(slot, stack) ?: Unit
    override fun removeStack(slot: Int, amount: Int): ItemStack =
        findAdjacentReactor()?.removeStack(slot, amount) ?: ItemStack.EMPTY
    override fun removeStack(slot: Int): ItemStack = findAdjacentReactor()?.removeStack(slot) ?: ItemStack.EMPTY
    override fun clear() = findAdjacentReactor()?.clear() ?: Unit
    override fun isEmpty(): Boolean = findAdjacentReactor()?.isEmpty() ?: true
    override fun markDirty() {
        findAdjacentReactor()?.markDirty()
        super.markDirty()
    }
    override fun canPlayerUse(player: PlayerEntity): Boolean =
        findAdjacentReactor()?.canPlayerUse(player) ?: false

    companion object {
        const val CHAMBER_TIER = 5

        fun tick(world: World, pos: BlockPos, state: BlockState, be: ReactorChamberBlockEntity) {
            // 不需要额外的 tick 逻辑，所有逻辑都由中心反应堆处理
        }
    }
}

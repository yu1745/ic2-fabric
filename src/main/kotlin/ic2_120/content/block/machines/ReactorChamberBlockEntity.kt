package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.ReactorChamberBlock
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage
import team.reborn.energy.api.base.SimpleSidedEnergyContainer

/**
 * 核反应仓方块实体。
 * 与核反应堆相邻时，共享反应堆的能量实例。
 * 实现了 ITieredMachine 接口，能量等级为 5。
 */
@ModBlockEntity(block = ReactorChamberBlock::class)
class ReactorChamberBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine {

    override val tier: Int = CHAMBER_TIER

    constructor(pos: BlockPos, state: BlockState) : this(
        ReactorChamberBlockEntity::class.type(),
        pos,
        state
    )

    /**
     * 查找相邻的核反应堆方块实体
     */
    private fun findAdjacentReactor(): NuclearReactorBlockEntity? {
        val world = world ?: return null
        for (dir in Direction.values()) {
            val neighborPos = pos.offset(dir)
            val neighborState = world.getBlockState(neighborPos)
            if (neighborState.block is ic2_120.content.block.NuclearReactorBlock) {
                val be = world.getBlockEntity(neighborPos) as? NuclearReactorBlockEntity
                if (be != null) return be
            }
        }
        return null
    }

    /**
     * 能量存储提供者
     */
    fun getEnergyStorage(side: Direction?): team.reborn.energy.api.EnergyStorage? {
        val reactor = findAdjacentReactor() ?: return null
        return reactor.sync as team.reborn.energy.api.EnergyStorage
    }

    companion object {
        /** 核反应仓的能量等级（5级，与核反应堆相同） */
        const val CHAMBER_TIER = 5

        fun tick(world: World, pos: BlockPos, state: BlockState, be: ReactorChamberBlockEntity) {
            // 不需要额外的 tick 逻辑，所有逻辑都由中心反应堆处理
        }
    }
}

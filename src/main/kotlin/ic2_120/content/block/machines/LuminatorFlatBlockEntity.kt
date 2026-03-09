package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.LuminatorFlatBlock
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.sync.LuminatorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 日光灯方块实体。无 GUI，仅消耗 EU 发光。
 * 电压等级 5（不限制电压），有电时发光等级 15。
 */
@ModBlockEntity(block = LuminatorFlatBlock::class)
class LuminatorFlatBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine {

    override val tier: Int = 5

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = LuminatorSync(syncedData) { world?.time }

    /** 每 CYCLE_TICKS 消耗 1 EU 的计数 */
    private var cycleTicks = 0

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(LuminatorFlatBlockEntity::class),
        pos,
        state
    )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(LuminatorSync.NBT_ENERGY_STORED).coerceIn(0L, LuminatorSync.ENERGY_CAPACITY)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        cycleTicks = nbt.getInt("CycleTicks")
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        syncedData.writeNbt(nbt)
        nbt.putLong(LuminatorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("CycleTicks", cycleTicks)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        pullEnergyFromNeighbors(world, pos, sync, LuminatorSync.MAX_INSERT)

        cycleTicks++
        if (cycleTicks >= LuminatorSync.CYCLE_TICKS) {
            cycleTicks = 0
            if (sync.amount >= LuminatorSync.ENERGY_PER_CYCLE) {
                sync.amount = (sync.amount - LuminatorSync.ENERGY_PER_CYCLE).coerceAtLeast(0L)
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                setActiveState(world, pos, state, true)
                markDirty()
                return
            }
        }

        if (sync.amount < LuminatorSync.ENERGY_PER_CYCLE) {
            setActiveState(world, pos, state, false)
        }
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(LuminatorFlatBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(LuminatorFlatBlock.ACTIVE, active))
        }
    }
}

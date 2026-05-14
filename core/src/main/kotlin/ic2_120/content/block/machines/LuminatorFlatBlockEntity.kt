package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.LuminatorFlatBlock
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.sync.LuminatorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.registry.RegistryWrapper

/**
 * 日光灯方块实体。无 GUI，100 EU 缓存，仅消耗 EU 发光。
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

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    /** 每 CYCLE_TICKS 消耗 1 EU 的计数 */
    private var cycleTicks = 0

    constructor(pos: BlockPos, state: BlockState) : this(
        LuminatorFlatBlockEntity::class.type(),
        pos,
        state
    )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(LuminatorSync.NBT_ENERGY_STORED))
        cycleTicks = nbt.getInt("CycleTicks")
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(LuminatorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("CycleTicks", cycleTicks)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        adjacentEnergyTransfer.tick()

        cycleTicks++
        if (cycleTicks >= LuminatorSync.CYCLE_TICKS) {
            cycleTicks = 0
            if (sync.consumeEnergy(LuminatorSync.ENERGY_PER_CYCLE) > 0L) {
                setActiveState(world, pos, state, true)
                markDirty()
                sync.syncCurrentTickFlow()
                return
            }
        }

        if (sync.amount < LuminatorSync.ENERGY_PER_CYCLE) {
            setActiveState(world, pos, state, false)
        }
        sync.syncCurrentTickFlow()
    }
    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(LuminatorFlatBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(LuminatorFlatBlock.ACTIVE, active))
        }
    }
}










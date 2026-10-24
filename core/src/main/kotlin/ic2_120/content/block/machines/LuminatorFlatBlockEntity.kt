package ic2_120.content.block.machines

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.LuminatorFlatBlock
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.sync.LuminatorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * 日光灯方块实体。无 GUI，100 EU 缓存，仅消耗 EU 发光。
 * 电压等级 5（不限制电压），有电时发光等级 15。
 */
@ModBlockEntity(block = LuminatorFlatBlock::class)
class LuminatorFlatBlockEntity(
    type: net.minecraft.world.level.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine {

    override val tier: Int = 5

    val syncedData = SyncedData()
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

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(LuminatorSync.NBT_ENERGY_STORED))
        cycleTicks = nbt.getInt("CycleTicks")
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(LuminatorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("CycleTicks", cycleTicks)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        adjacentEnergyTransfer.tick()

        cycleTicks++
        if (cycleTicks >= LuminatorSync.CYCLE_TICKS) {
            cycleTicks = 0
            if (sync.consumeEnergy(LuminatorSync.ENERGY_PER_CYCLE) > 0L) {
                setActiveState(world, pos, state, true)
                setChanged()
                sync.syncCurrentTickFlow()
                return
            }
        }

        if (sync.amount < LuminatorSync.ENERGY_PER_CYCLE) {
            setActiveState(world, pos, state, false)
        }
        sync.syncCurrentTickFlow()
    }
    private fun setActiveState(world: Level, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.getValue(LuminatorFlatBlock.ACTIVE) != active) {
            world.setBlockAndUpdate(pos, state.setValue(LuminatorFlatBlock.ACTIVE, active))
        }
    }
}










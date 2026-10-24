package ic2_120.content.block.machines

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.KineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.block.transmission.pullKuFromNeighbors
import ic2_120.content.screen.KineticGeneratorScreenHandler
import ic2_120.content.sync.KineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.nbt.CompoundTag

import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.network.FriendlyByteBuf
import io.netty.buffer.Unpooled

@ModBlockEntity(block = KineticGeneratorBlock::class)
class KineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), ITieredMachine, IGenerator, IKineticMachinePort,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    companion object {
        private const val START_THRESHOLD_KU = 64
        private const val STOP_THRESHOLD_KU = 48
    }

    override val tier: Int = 3
    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = KineticGeneratorBlock.ACTIVE

    private val syncedData = SyncedData()
    private var lastKuInputTick: Long = Long.MIN_VALUE
    private var inputKuThisTick: Int = 0
    private var outputEuThisTick: Int = 0
    private var kuRemainder: Int = 0

    @RegisterEnergy
    val sync = KineticGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    constructor(pos: BlockPos, state: BlockState) : this(
        KineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container? = null

    override fun canInputKuFrom(side: Direction): Boolean {
        val world = world ?: return false
        val facing = world.getBlockState(pos).getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)
        // 正面为输入口。
        return side == facing
    }

    override fun getStoredKu(side: Direction): Int {
        if (!canInputKuFrom(side)) return 0
        return sync.amount
            .coerceIn(0L, KineticGeneratorSync.ENERGY_CAPACITY)
            .times(KineticGeneratorSync.KU_PER_EU.toLong())
            .plus(kuRemainder.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    override fun getKuCapacity(side: Direction): Int {
        if (!canInputKuFrom(side)) return 0
        return KineticGeneratorSync.ENERGY_CAPACITY
            .times(KineticGeneratorSync.KU_PER_EU.toLong())
            .plus((KineticGeneratorSync.KU_PER_EU - 1).toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    override fun getMaxInsertableKu(side: Direction): Int {
        if (!canInputKuFrom(side)) return 0
        val bufferLimited = getRemainingKuBuffer(side)
        return minOf(bufferLimited, KineticGeneratorSync.MAX_INPUT_KU).coerceAtLeast(0)
    }

    override fun insertKu(side: Direction, amount: Int, simulate: Boolean): Int {
        if (!canInputKuFrom(side) || amount <= 0) return 0
        val acceptedKu = minOf(amount, getMaxInsertableKu(side))
        if (acceptedKu <= 0 || simulate) return acceptedKu

        val world = world
        if (world != null) {
            if (lastKuInputTick != world.time) {
                lastKuInputTick = world.time
                inputKuThisTick = 0
                outputEuThisTick = 0
            }
        }
        inputKuThisTick += acceptedKu

        val totalKu = kuRemainder + acceptedKu
        val producedEu = (totalKu / KineticGeneratorSync.KU_PER_EU).toLong()
        kuRemainder = totalKu % KineticGeneratorSync.KU_PER_EU
        var generatedEu = 0L
        if (producedEu > 0L) {
            generatedEu = sync.generateEnergy(producedEu)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            outputEuThisTick += generatedEu.toInt()
        }
        sync.currentKu = inputKuThisTick
        sync.outputEu = outputEuThisTick
        sync.isGenerating = 1
        markDirtyThrottled()
        return acceptedKu
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        val transferredEu = adjacentEnergyTransfer.tick()
        val pulledKu = pullKuFromNeighbors(world, pos, this)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        val hasInputThisTick = lastKuInputTick == world.time
        if (!hasInputThisTick) {
            inputKuThisTick = 0
            outputEuThisTick = 0
            sync.currentKu = 0
            sync.outputEu = 0
            sync.isGenerating = 0
        }

        val runThreshold = if (state.getValue(KineticGeneratorBlock.ACTIVE)) STOP_THRESHOLD_KU else START_THRESHOLD_KU
        val active = sync.currentKu >= runThreshold
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
        markDirtyThrottled()
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.kinetic_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        KineticGeneratorScreenHandler(
            syncId,
            playerInventory,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(KineticGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, KineticGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        kuRemainder = nbt.getInt("KuRemainder").coerceIn(0, KineticGeneratorSync.KU_PER_EU - 1)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(KineticGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("KuRemainder", kuRemainder.coerceIn(0, KineticGeneratorSync.KU_PER_EU - 1))
    }
}

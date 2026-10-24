package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.screen.WindGeneratorScreenHandler
import ic2_120.content.sync.WindGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 风力发电机方块实体。
 *
 * 发电量算法：方块 Y > 74 时固定 3 EU/t，否则不发电。
 */
@ModBlockEntity(block = WindGeneratorBlock::class)
class WindGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    companion object {
        const val INVENTORY_SIZE = 1
        const val GENERATOR_TIER = 1
        const val BATTERY_SLOT = 0

        private const val MIN_GENERATION_Y = 74
        private const val OUTPUT_MILLI_EU_PER_TICK = 3_000
        const val STATUS_GENERATING = 0
        const val STATUS_TOO_LOW = 1
    }

    override val tier: Int = GENERATOR_TIER

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = WindGeneratorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.loop(
        soundId = "generator.wind.loop",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    private val inventory = NonNullList.withSize(1, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { canPlaceItem(BATTERY_SLOT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(BATTERY_SLOT),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = WindGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    /** 当前输出（milli EU/t） */
    private var currentOutputMilliEuPerTick: Int = 0

    /** 分数 EU 累积（milli EU），满 1000 时产生 1 EU */
    private var euAccum: Int = 0

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0 }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        WindGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container = this

    override fun getContainerSize(): Int = 1
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            BATTERY_SLOT -> stack.canBeCharged()
            else -> false
        }
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = canPlaceInSlot(slot, stack)

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.count)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.wind_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        WindGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(WindGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, WindGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        currentOutputMilliEuPerTick = nbt.getInt("CurrentOutputMilliEuPerTick").coerceIn(0, 20000)
        euAccum = nbt.getInt("EuAccum").coerceIn(0, 999)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(WindGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("CurrentOutputMilliEuPerTick", currentOutputMilliEuPerTick)
        nbt.putInt("EuAccum", euAccum)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        updatePowerOutput(pos)

        val canGenerate = currentOutputMilliEuPerTick > 0
        sync.isGenerating = if (canGenerate) 1 else 0
        sync.status = if (canGenerate) STATUS_GENERATING else STATUS_TOO_LOW

        if (canGenerate) {
            val space = (WindGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            if (space > 0L) {
                // 每 tick 产生 currentOutputMilliEuPerTick/1000 EU（支持小数）
                euAccum += currentOutputMilliEuPerTick
                var euToAdd = 0L
                while (euAccum >= 1000 && space > euToAdd) {
                    euAccum -= 1000
                    euToAdd++
                }
                if (euToAdd > 0L) {
                    sync.generateEnergy(euToAdd)
                    sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                    setChanged()
                }
            }
        }

        batteryCharger.tick()

        val active = canGenerate
        setActiveState(world, pos, state, active)
        // 同步当前 tick 的实际输出/输入
        sync.syncCurrentTickFlow()

    }

    private fun updatePowerOutput(pos: BlockPos) {
        currentOutputMilliEuPerTick = if (pos.y > MIN_GENERATION_Y) {
            OUTPUT_MILLI_EU_PER_TICK
        } else {
            0
        }
    }
}

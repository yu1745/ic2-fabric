package ic2_120.content.block.machines

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
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

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
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, IGenerator,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

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

    override val activeProperty: net.minecraft.state.property.BooleanProperty = WindGeneratorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.loop(
        soundId = "generator.wind.loop",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(BATTERY_SLOT), matcher = { isValid(BATTERY_SLOT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(BATTERY_SLOT),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = WindGeneratorSync(
        schema = syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
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

    override fun getInventory(): Inventory = this

    override fun size(): Int = 1
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            BATTERY_SLOT -> stack.canBeCharged()
            else -> false
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = canPlaceInSlot(slot, stack)

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.wind_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        WindGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(WindGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, WindGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        currentOutputMilliEuPerTick = nbt.getInt("CurrentOutputMilliEuPerTick").coerceIn(0, 20000)
        euAccum = nbt.getInt("EuAccum").coerceIn(0, 999)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(WindGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("CurrentOutputMilliEuPerTick", currentOutputMilliEuPerTick)
        nbt.putInt("EuAccum", euAccum)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
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
                    markDirty()
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

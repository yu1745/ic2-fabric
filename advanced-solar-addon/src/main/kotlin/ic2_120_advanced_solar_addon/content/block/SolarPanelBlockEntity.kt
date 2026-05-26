package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.sync.SolarPanelSync
import ic2_120_advanced_solar_addon.content.screen.SolarPanelScreenHandler
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.content.syncs.SyncedData
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

enum class GenerationState {
    NONE, NIGHT, DAY
}

abstract class SolarPanelBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<out SolarPanelBlockEntity>,
    pos: BlockPos,
    state: BlockState,
    val dayPower: Int,
    val nightPower: Int,
    maxStorage: Long,
    override val tier: Int,
    activeProperty: BooleanProperty
) : MachineBlockEntity(type, pos, state), IGenerator, ExtendedScreenHandlerFactory, Inventory {

    companion object {
        private const val CHARGE_SLOTS = 4
        private const val INVENTORY_NBT_KEY = "charge_inventory"
        /** 6:20 AM ≈ 333 ticks（0 = 6:00 AM，20 分钟 ≈ 333 ticks） */
        private const val DAY_START_TICK = 333
        /** 17:45 PM = 11750 ticks（11*1000 + 750） */
        private const val DAY_END_TICK = 11750
    }

    @Suppress("unused")
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = SolarPanelSync(
        schema = syncedData,
        capacity = maxStorage,
        tier = tier,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    val maxStorage: Long = maxStorage

    override val activeProperty: BooleanProperty = activeProperty

    var generationState: GenerationState = GenerationState.NONE
        private set
    private var ticker: Int = 0
    private val tickRate: Int = 128

    // ====== Inventory ======

    private val inventory = DefaultedList.ofSize(CHARGE_SLOTS, ItemStack.EMPTY)

    private val chargeSlotMatcher: (ItemStack) -> Boolean = { stack ->
        val item = stack.item
        when {
            item is IElectricTool -> true
            item is IBatteryItem && item.canCharge -> item.tier <= tier
            else -> false
        }
    }

    private val chargeSlotIndices = IntArray(CHARGE_SLOTS) { it }

    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { 1 },
        slotValidator = { _, stack -> chargeSlotMatcher(stack) },
        insertRoutes = listOf(
            ItemInsertRoute(
                slotIndices = chargeSlotIndices,
                matcher = chargeSlotMatcher,
                maxPerSlot = 1
            )
        ),
        extractSlots = chargeSlotIndices,
        markDirty = { markDirty() }
    )

    private val chargerComponents = (0 until CHARGE_SLOTS).map { slot ->
        BatteryChargerComponent(
            inventory = this,
            batterySlot = slot,
            machineTierProvider = { tier },
            machineEnergyProvider = { sync.amount },
            extractEnergy = { requested -> sync.extractEnergy(requested) },
            canChargeNow = { true }
        )
    }

    override fun getInventory(): Inventory? = this

    open fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()

        if (ticker++ % tickRate == 0) {
            checkSky()
        }

        val canGenerate = generationState != GenerationState.NONE
        when (generationState) {
            GenerationState.DAY -> sync.generateEnergy(dayPower.toLong())
            GenerationState.NIGHT -> sync.generateEnergy(nightPower.toLong())
            GenerationState.NONE -> {}
        }

        sync.isGenerating = if (canGenerate) 1 else 0
        sync.generationState = generationState.ordinal
        sync.dayPower = dayPower
        sync.nightPower = nightPower
        sync.maxOutput = ic2_120.content.energy.EnergyTier.euPerTickFromTier(tier).toInt()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        sync.syncCurrentTickFlow()

        for (charger in chargerComponents) {
            charger.tick()
        }

        setActiveState(world, pos, state, canGenerate)
        markDirty()
    }

    private fun checkSky() {
        val world = this.world ?: return
        val pos = this.pos

        if (!hasSkyAccess(world, pos)) {
            generationState = GenerationState.NONE
            return
        }

        if (world.registryKey != World.OVERWORLD) {
            generationState = GenerationState.NONE
            return
        }

        val time = world.timeOfDay % 24000
        val isRaining = world.isRaining || world.isThundering
        val canRain = world.getBiome(pos).value().hasPrecipitation()
        val isDaytime = time in DAY_START_TICK..DAY_END_TICK

        generationState = when {
            isDaytime && (!canRain || !isRaining) -> GenerationState.DAY
            isDaytime && canRain && isRaining -> GenerationState.NONE
            !isDaytime -> GenerationState.NIGHT
            else -> GenerationState.NONE
        }

        markDirty()
    }

    /**
     * 检查正上方是否有天空可见（无非透明方块遮挡）。
     * 参考 ic2-fabric 的 SolarGeneratorBlockEntity 实现。
     */
    private fun hasSkyAccess(world: World, basePos: BlockPos): Boolean {
        val topY = world.topY
        var y = basePos.y + 1
        while (y < topY) {
            val pos = BlockPos(basePos.x, y, basePos.z)
            val blockState = world.getBlockState(pos)
            if (blockState.isOpaqueFullCube(world, pos)) return false
            y++
        }
        return true
    }

    // ====== Inventory interface ======

    override fun size(): Int = CHARGE_SLOTS
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    // ====== ExtendedScreenHandlerFactory ======

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120_advanced_solar_addon.${getBlockName()}")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SolarPanelScreenHandler(
            syncId, playerInventory,
            ScreenHandlerContext.create(world!!, pos),
            syncedData,
            this, CHARGE_SLOTS, tier,
            itemStorage
        )

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeVarInt(CHARGE_SLOTS)
        buf.writeVarInt(tier)
    }

    protected open fun getBlockName(): String = "advanced_solar_panel"

    // ====== NBT ======

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(SolarPanelSync.NBT_ENERGY).coerceIn(0L, sync.capacity))
        generationState = GenerationState.values()[nbt.getInt("state").coerceIn(0, 2)]
        syncedData.readNbt(nbt)
        Inventories.readNbt(nbt.getCompound(INVENTORY_NBT_KEY), inventory)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putLong(SolarPanelSync.NBT_ENERGY, sync.amount)
        nbt.putInt("state", generationState.ordinal)
        syncedData.writeNbt(nbt)
        nbt.put(INVENTORY_NBT_KEY, Inventories.writeNbt(NbtCompound(), inventory))
    }
}

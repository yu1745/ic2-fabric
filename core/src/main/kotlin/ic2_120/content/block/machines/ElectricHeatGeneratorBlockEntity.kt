package ic2_120.content.block.machines

import ic2_120.compat.time

import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.ElectricHeatGeneratorBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.IRedstoneControlSupport
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.screen.ElectricHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * 电力加热机：
 * - 10 个线圈槽，每槽 1 个线圈
 * - 1 EU -> 1 HU
 * - 单槽 10HU/t，满槽 100HU/t
 * - Tier 4，缓存 10k EU
 * - 支持红石控制
 */
@ModBlockEntity(block = ElectricHeatGeneratorBlock::class)
class ElectricHeatGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatGeneratorBlockEntityBase(type, pos, state), Container, IRoutedSidedInventory, IRedstoneControlSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = ElectricHeatGeneratorBlock.ACTIVE

    companion object {
        const val SLOT_COUNT = 11
        const val SLOT_DISCHARGING = 10
        private const val HU_PER_COIL_PER_TICK = 10L
    }

    override val tier: Int = 4
    override var redstoneInverted: Boolean = false

    private val inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute((0 until SLOT_DISCHARGING).toList().toIntArray(), matcher = { !it.isEmpty && it.item == coilItem }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust) }, maxPerSlot = 1)
        ),
        extractSlots = IntArray(SLOT_COUNT) { it },
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    private val coilItem by lazy { BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "coil")) }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < ElectricHeatGeneratorSync.ENERGY_CAPACITY }
    )

    val syncedData = SyncedData()
    override val heatFlow = HeatFlowSync(syncedData, this)
    @RegisterEnergy
    val sync = ElectricHeatGeneratorSync(
        schema = syncedData,
        currentTickProvider = { world?.time },
        heatFlow = heatFlow
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    constructor(pos: BlockPos, state: BlockState) : this(
        ElectricHeatGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun writeScreenOpeningData(player: net.minecraft.server.level.ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.electric_heat_generator")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        ElectricHeatGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun getInventory(): Container = this
    override fun size(): Int = SLOT_COUNT
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clear() = inventory.clear()
    override fun stillValid(player: net.minecraft.world.entity.player.Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        in 0 until SLOT_DISCHARGING -> !stack.isEmpty && stack.item == coilItem
        SLOT_DISCHARGING -> !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
        else -> false
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING) {
            if (stack.isEmpty || stack.item !is IBatteryItem) return
            val single = stack.copy()
            single.count = 1
            inventory[slot] = single
            setChanged()
            return
        }
        if (!stack.isEmpty && stack.item != coilItem) return
        val stackToSet = if (!stack.isEmpty && stack.count > 1) {
            val single = stack.copy()
            single.count = 1
            single
        } else {
            stack
        }
        inventory[slot] = stackToSet
        setChanged()
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ElectricHeatGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, ElectricHeatGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ElectricHeatGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }

    override fun preGenerate(world: Level, pos: BlockPos, state: BlockState) {
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    private fun extractFromDischargingSlot() {
        val space = (ElectricHeatGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
        val request = minOf(space, ElectricHeatGeneratorSync.MAX_INSERT)
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        setChanged()
    }

    /** 当前安装的线圈数量。 */
    fun getCoilCount(): Int = inventory.count { !it.isEmpty && it.item == coilItem }

    override fun generateHeat(world: Level, pos: BlockPos, state: BlockState): Long {
        val redstoneAllowsRun = RedstoneControlComponent.canRun(world, pos, this)
        val coilCount = getCoilCount()
        val maxHuPerTick = coilCount * HU_PER_COIL_PER_TICK

        var generatedThisTick = 0L
        if (redstoneAllowsRun && maxHuPerTick > 0L) {
            val euNeed = maxHuPerTick
            val consumed = sync.consumeEnergy(minOf(euNeed, sync.amount))
            if (consumed > 0L) {
                generatedThisTick = consumed
            }
        }
        return generatedThisTick
    }

    override fun syncAdditionalData() {
        sync.syncCurrentTickFlow()
    }

    override fun shouldActivate(generatedHeat: Long, hasValidConsumer: Boolean): Boolean =
        generatedHeat > 0L && hasValidConsumer

    override fun getActiveState(state: BlockState): Boolean =
        state.getValue(ElectricHeatGeneratorBlock.ACTIVE)

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        tickHeatMachine(world, pos, state)
    }
}

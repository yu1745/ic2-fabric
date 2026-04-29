package ic2_120.content.block.machines

import ic2_120.config.Ic2Config
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.UuScannerBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.UuScannerScreenHandler
import ic2_120.content.sync.UuScannerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.uu.findUniqueAdjacentPatternStorage
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.ceil
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

@ModBlockEntity(block = UuScannerBlock::class)
class UuScannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty = UuScannerBlock.ACTIVE
    override val tier: Int = UuScannerSync.UU_SCANNER_TIER
    override fun getInventory(): Inventory = this

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_DISCHARGING = 1
        const val SLOT_UPGRADE_0 = 2
        const val SLOT_UPGRADE_1 = 3
        const val SLOT_UPGRADE_2 = 4
        const val SLOT_UPGRADE_3 = 5
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 6
        private const val NBT_LAST_COMPLETED_ITEM = "LastCompletedItem"
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isValid(SLOT_INPUT, it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT, SLOT_DISCHARGING) + SLOT_UPGRADE_INDICES,
        markDirty = { markDirty() }
    )
    private var lastCompletedItemId: String = ""

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = UuScannerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(UuScannerSync.UU_SCANNER_TIER + voltageTierBonus) }
    )

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { UuScannerSync.UU_SCANNER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(UuScannerBlockEntity::class.type(), pos, state)

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        val previousId = if (slot == SLOT_INPUT) Registries.ITEM.getId(inventory[slot].item).toString() else ""
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        if (slot == SLOT_INPUT) {
            val newId = if (stack.isEmpty) "" else Registries.ITEM.getId(stack.item).toString()
            if (newId != previousId) {
                lastCompletedItemId = ""
                sync.progress = 0
            }
        }
        markDirty()
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> !stack.isEmpty && stack.item !is IBatteryItem
        SLOT_DISCHARGING -> !stack.isEmpty && stack.item is IBatteryItem
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.uu_scanner")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        UuScannerScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(UuScannerSync.NBT_ENERGY_STORED))
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        lastCompletedItemId = nbt.getString(NBT_LAST_COMPLETED_ITEM)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(UuScannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putString(NBT_LAST_COMPLETED_ITEM, lastCompletedItemId)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        val storage = findUniqueAdjacentPatternStorage(world, pos)
        if (storage == null) {
            sync.progress = 0
            sync.currentCostUb = 0
            sync.status = UuScannerSync.STATUS_NO_STORAGE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) {
            sync.progress = 0
            sync.currentCostUb = 0
            sync.status = UuScannerSync.STATUS_NO_INPUT
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val itemId = Registries.ITEM.getId(input.item).toString()
        val template = Ic2Config.getReplicationTemplate(itemId)
        if (template == null) {
            sync.progress = 0
            sync.currentCostUb = 0
            sync.status = UuScannerSync.STATUS_NOT_WHITELISTED
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        sync.currentCostUb = template.uuCostUb
        if (lastCompletedItemId == itemId && sync.progress == 0) {
            sync.status = UuScannerSync.STATUS_COMPLETE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        val nextProgress = (sync.progress + progressIncrement).coerceAtMost(UuScannerSync.PROGRESS_MAX)
        val need = ceil(UuScannerSync.ENERGY_PER_TICK * energyMultiplier.toDouble()).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress = nextProgress
            sync.status = UuScannerSync.STATUS_SCANNING
            if (sync.progress >= UuScannerSync.PROGRESS_MAX) {
                storage.addOrSelectTemplate(template)
                sync.progress = 0
                sync.status = UuScannerSync.STATUS_COMPLETE
                lastCompletedItemId = itemId
            }
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            sync.status = UuScannerSync.STATUS_NO_ENERGY
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return
        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
}

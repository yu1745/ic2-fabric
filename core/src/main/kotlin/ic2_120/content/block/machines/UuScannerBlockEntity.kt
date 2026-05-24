package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.content.block.UuScannerBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.UuScannerScreenHandler
import ic2_120.content.sync.UuScannerSync
import ic2_120.content.syncs.SyncedData
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
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.ceil

@ModBlockEntity(block = UuScannerBlock::class)
class UuScannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    override val activeProperty = UuScannerBlock.ACTIVE
    override val tier: Int = UuScannerSync.UU_SCANNER_TIER
    override fun getInventory(): Inventory = this

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_DISCHARGING = 1
        const val SLOT_CRYSTAL = 2
        const val INVENTORY_SIZE = 3
        private const val NBT_LAST_COMPLETED_ITEM = "LastCompletedItem"
        private val crystalMemoryId = Identifier(Ic2_120.MOD_ID, "crystal_memory")
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isValid(SLOT_INPUT, it) }),
            ItemInsertRoute(intArrayOf(SLOT_CRYSTAL), matcher = { it.isEmpty || isCrystalMemory(it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT, SLOT_DISCHARGING, SLOT_CRYSTAL),
        markDirty = { markDirty() }
    )
    private var lastCompletedItemId: String = ""

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = UuScannerSync(
        syncedData,
        { world?.time },
        { 0L },
        { UuScannerSync.MAX_INSERT }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
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
        SLOT_CRYSTAL -> stack.isEmpty || isCrystalMemory(stack)
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.uu_scanner")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        UuScannerScreenHandler(syncId, playerInventory, this, pos, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(UuScannerSync.NBT_ENERGY_STORED))
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        lastCompletedItemId = nbt.getString(NBT_LAST_COMPLETED_ITEM)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(UuScannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putString(NBT_LAST_COMPLETED_ITEM, lastCompletedItemId)
    }

    fun deleteTemplate() {
        val world = world ?: return
        val storage = findUniqueAdjacentPatternStorage(world, pos) ?: return
        storage.removeSelectedTemplate()
        sync.status = UuScannerSync.STATUS_NO_INPUT
        lastCompletedItemId = ""
        sync.currentCostUb = 0
        markDirty()
    }

    fun saveTemplateToCrystal() {
        val world = world ?: return
        val crystalStack = getStack(SLOT_CRYSTAL)
        if (!isCrystalMemory(crystalStack)) return
        val storage = findUniqueAdjacentPatternStorage(world, pos) ?: return
        storage.exportSelectedTemplateToCrystal()
        storage.removeSelectedTemplate()
        sync.status = UuScannerSync.STATUS_NO_INPUT
        lastCompletedItemId = ""
        sync.currentCostUb = 0
        markDirty()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
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
            sync.status = if (lastCompletedItemId.isNotBlank()) UuScannerSync.STATUS_COMPLETE else UuScannerSync.STATUS_NO_INPUT
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

        val need = ceil(UuScannerSync.ENERGY_PER_TICK.toDouble()).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress = (sync.progress + 1).coerceAtMost(UuScannerSync.PROGRESS_MAX)
            sync.status = UuScannerSync.STATUS_SCANNING
            if (sync.progress >= UuScannerSync.PROGRESS_MAX) {
                storage.addOrSelectTemplate(template)
                input.decrement(1)
                if (input.isEmpty) setStack(SLOT_INPUT, ItemStack.EMPTY)
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

    private fun isCrystalMemory(stack: ItemStack): Boolean =
        !stack.isEmpty && Registries.ITEM.getId(stack.item) == crystalMemoryId
}

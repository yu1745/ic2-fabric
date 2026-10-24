package ic2_120.content.block.machines

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
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
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.content.uu.findUniqueAdjacentPatternStorage
import ic2_120.content.uu.setUuTemplate
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import kotlin.math.ceil

@ModBlockEntity(block = UuScannerBlock::class)
class UuScannerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    override val activeProperty = UuScannerBlock.ACTIVE
    override val tier: Int = UuScannerSync.UU_SCANNER_TIER
    override fun getInventory(): Container = this

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_DISCHARGING = 1
        const val SLOT_CRYSTAL = 2
        const val INVENTORY_SIZE = 3
        private const val NBT_LAST_COMPLETED_ITEM = "LastCompletedItem"
        private val crystalMemoryId = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "crystal_memory")
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { canPlaceItem(SLOT_INPUT, it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_CRYSTAL), matcher = { it.isEmpty || isCrystalMemory(it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT, SLOT_DISCHARGING, SLOT_CRYSTAL),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    private var lastCompletedItemId: String = ""
    private var cachedTemplate: UuTemplateEntry? = null

    val syncedData = SyncedData()

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

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        val previousId = if (slot == SLOT_INPUT) BuiltInRegistries.ITEM.getKey(inventory[slot].item).toString() else ""
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        if (slot == SLOT_INPUT) {
            val newId = if (stack.isEmpty) "" else BuiltInRegistries.ITEM.getKey(stack.item).toString()
            if (newId != previousId) {
                lastCompletedItemId = ""
                sync.progress = 0
            }
        }
        setChanged()
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun setChanged() { super.setChanged() }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> !stack.isEmpty && stack.item !is IBatteryItem
        SLOT_DISCHARGING -> !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
        SLOT_CRYSTAL -> stack.isEmpty || isCrystalMemory(stack)
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getCount())
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.uu_scanner")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        UuScannerScreenHandler(syncId, playerInventory, this, pos, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(UuScannerSync.NBT_ENERGY_STORED))
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
        lastCompletedItemId = nbt.getString(NBT_LAST_COMPLETED_ITEM)
        cachedTemplate = if (nbt.contains("CachedTemplate")) UuTemplateEntry.fromNbt(nbt.getCompound("CachedTemplate")) else null
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(UuScannerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putString(NBT_LAST_COMPLETED_ITEM, lastCompletedItemId)
        cachedTemplate?.let { nbt.put("CachedTemplate", it.toNbt()) }
    }

    fun deleteTemplate() {
        cachedTemplate = null
        sync.cachedItemRawId = 0
        sync.currentCostUb = 0
        sync.status = UuScannerSync.STATUS_NO_INPUT
        setChanged()
    }

    fun saveTemplateToCrystal() {
        val template = cachedTemplate ?: return
        val world = world ?: return

        // 优先存入水晶
        val crystalStack = getItem(SLOT_CRYSTAL)
        if (isCrystalMemory(crystalStack)) {
            crystalStack.setUuTemplate(template)
        } else {
            // 水晶槽无水晶 → 存入邻接存储机
            val storage = findUniqueAdjacentPatternStorage(world, pos) ?: return
            storage.addOrSelectTemplate(template)
        }

        cachedTemplate = null
        sync.cachedItemRawId = 0
        sync.currentCostUb = 0
        sync.status = UuScannerSync.STATUS_NO_INPUT
        setChanged()
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        // 缓存中有模板 → 等待存入/删除，不允许新扫描
        if (cachedTemplate != null) {
            sync.status = UuScannerSync.STATUS_COMPLETE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val storage = findUniqueAdjacentPatternStorage(world, pos)
        val useCrystal = storage == null && isCrystalMemory(getItem(SLOT_CRYSTAL))
        if (storage == null && !useCrystal) {
            sync.progress = 0
            sync.currentCostUb = 0
            sync.status = UuScannerSync.STATUS_NO_STORAGE
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val input = getItem(SLOT_INPUT)
        if (input.isEmpty) {
            sync.progress = 0
            sync.currentCostUb = 0
            sync.status = UuScannerSync.STATUS_NO_INPUT
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val itemId = BuiltInRegistries.ITEM.getKey(input.item).toString()
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
        sync.cachedItemRawId = BuiltInRegistries.ITEM.getId(input.item)

        val need = ceil(UuScannerSync.ENERGY_PER_TICK.toDouble()).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress = (sync.progress + 1).coerceAtMost(UuScannerSync.PROGRESS_MAX)
            sync.status = UuScannerSync.STATUS_SCANNING
            if (sync.progress >= UuScannerSync.PROGRESS_MAX) {
                // 扫描完成 → 模板缓存至内置缓存位
                cachedTemplate = template
                input.shrink(1)
                if (input.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)
                sync.progress = 0
                sync.status = UuScannerSync.STATUS_COMPLETE
            }
            setChanged()
            setActiveState(world, pos, state, true)
        } else {
            sync.status = UuScannerSync.STATUS_NO_ENERGY
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        setChanged()
    }

    private fun isCrystalMemory(stack: ItemStack): Boolean =
        !stack.isEmpty && BuiltInRegistries.ITEM.getKey(stack.item) == crystalMemoryId
}

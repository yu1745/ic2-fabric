package ic2_120.content.block.machines

import ic2_120.content.block.StirlingGeneratorBlock
import ic2_120.content.block.IGenerator
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.StirlingGeneratorScreenHandler
import ic2_120.content.sync.StirlingGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.heat.IHeatConsumer
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
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

@ModBlockEntity(block = StirlingGeneratorBlock::class)
class StirlingGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatConsumerBlockEntityBase(type, pos, state), Inventory, IGenerator, Storage<ItemVariant>, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = StirlingGeneratorBlock.ACTIVE

    companion object {
        const val STIRLING_TIER = 2
        const val BATTERY_SLOT = 0
        const val INVENTORY_SIZE = 1

        private const val NBT_HEAT_BUFFERED = "HeatBuffered"

        const val HU_PER_EU = 2L
        const val MAX_OUTPUT_EU_PER_TICK = 50L
        const val MAX_HEAT_PER_TICK = MAX_OUTPUT_EU_PER_TICK * HU_PER_EU
    }

    override val tier: Int = STIRLING_TIER
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
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
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = StirlingGeneratorSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time }
    )
    private val batteryCharger = BatteryChargerComponent(
        inventory = this,
        batterySlot = BATTERY_SLOT,
        machineTierProvider = { tier },
        machineEnergyProvider = { sync.amount },
        extractEnergy = { requested -> sync.consumeEnergy(requested) },
        canChargeNow = { sync.amount > 0L }
    )

    private var heatBuffered: Long = 0L

    constructor(pos: BlockPos, state: BlockState) : this(
        StirlingGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean =
        slot == BATTERY_SLOT && !stack.isEmpty && stack.canBeCharged()

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
        itemStorage.insert(resource, maxAmount, transaction)

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
        itemStorage.extract(resource, maxAmount, transaction)

    override fun iterator(): MutableIterator<StorageView<ItemVariant>> = itemStorage.iterator()

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.stirling_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        StirlingGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(StirlingGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, StirlingGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        heatBuffered = nbt.getLong(NBT_HEAT_BUFFERED)
        sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(StirlingGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_HEAT_BUFFERED, heatBuffered)
    }

    override fun receiveHeatInternal(hu: Long): Long {
        if (hu <= 0L) return 0L
        val toAdd = hu.coerceAtMost(MAX_HEAT_PER_TICK - heatBuffered)
        if (toAdd <= 0L) {
            return 0L
        }
        heatBuffered += toAdd
        sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        markDirty()
        return toAdd
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

        val euToGenerate = (heatBuffered / HU_PER_EU).coerceAtMost(MAX_OUTPUT_EU_PER_TICK)
        if (euToGenerate > 0L) {
            val space = (StirlingGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            val actualGenerate = minOf(euToGenerate, space)
            if (actualGenerate > 0L) {
                sync.generateEnergy(actualGenerate)
                heatBuffered -= actualGenerate * HU_PER_EU
                sync.heatBuffered = heatBuffered.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                markDirty()
            }
        }
        batteryCharger.tick()

        val active = sync.amount < StirlingGeneratorSync.ENERGY_CAPACITY &&
            heatBuffered > 0L &&
            hasValidHeatSource()
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun hasValidHeatSource(): Boolean {
        val world = world ?: return false
        val myFace = getHeatTransferFace()
        val neighborPos = pos.offset(myFace)
        val neighbor = world.getBlockEntity(neighborPos) as? ic2_120.content.heat.IHeatNode ?: return false
        val neighborFace = neighbor.getHeatTransferFace()
        return neighborFace == myFace.opposite
    }

    fun onNeighborUpdate() {
        markDirty()
    }
}

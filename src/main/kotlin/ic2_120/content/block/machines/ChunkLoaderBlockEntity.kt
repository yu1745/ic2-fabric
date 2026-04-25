package ic2_120.content.block.machines

import ic2_120.content.block.ChunkLoaderBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.ChunkLoaderScreenHandler
import ic2_120.content.sync.ChunkLoaderSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerChunkManager
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.registry.RegistryWrapper
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * 区块加载器方块实体。
 * 使用 ChunkTicketType.FORCED 强制加载周围区块，耗能 1 EU/tick 每区块。
 */
@ModBlockEntity(block = ChunkLoaderBlock::class)
class ChunkLoaderBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = ChunkLoaderBlock.ACTIVE

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = 1

    companion object {
        const val SLOT_DISCHARGING = 0
        const val INVENTORY_SIZE = 1
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_DISCHARGING),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = ChunkLoaderSync(syncedData) { world?.time }

    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < sync.capacity }
    )

    /** 当前是否已添加 chunk ticket（用于避免重复添加） */
    private var ticketsActive = false

    constructor(pos: BlockPos, state: BlockState) : this(
        ChunkLoaderBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean =
        slot == SLOT_DISCHARGING && !stack.isEmpty && stack.item is IBatteryItem

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.chunk_loader")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ChunkLoaderScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ChunkLoaderSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.range = nbt.getInt("Range").coerceIn(0, 2)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(ChunkLoaderSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("Range", sync.range)
    }

    override fun markRemoved() {
        releaseChunks()
        super.markRemoved()
    }

    /** 释放所有强制加载的区块 */
    fun releaseChunks() {
        if (!ticketsActive) return
        val sw = world as? ServerWorld ?: return
        val cm = sw.chunkManager as? ServerChunkManager ?: return
        val center = ChunkPos(pos)
        val radius = sync.getTicketRadius()
        cm.removeTicket(ChunkTicketType.FORCED, center, radius, center)
        ticketsActive = false
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        val chunkCount = sync.getChunkCount()
        val needPerTick = ChunkLoaderSync.EU_PER_CHUNK_PER_TICK * chunkCount

        if (sync.amount < needPerTick) {
            // 能量不足，停止加载
            if (ticketsActive) {
                releaseChunks()
                setActiveState(world, pos, state, false)
            }
            sync.syncCurrentTickFlow()
            return
        }

        // 消耗能量并维持加载
        if (sync.consumeEnergy(needPerTick) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            if (!ticketsActive) {
                val sw = world as? ServerWorld
                val cm = sw?.chunkManager as? ServerChunkManager
                if (cm != null) {
                    val center = ChunkPos(pos)
                    val radius = sync.getTicketRadius()
                    cm.addTicket(ChunkTicketType.FORCED, center, radius, center)
                    ticketsActive = true
                }
            }
            setActiveState(world, pos, state, true)
            markDirty()
        } else {
            if (ticketsActive) releaseChunks()
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.capacity - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return
        val request = minOf(space, ChunkLoaderSync.MAX_INSERT)
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
}

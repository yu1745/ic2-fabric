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
import net.minecraft.network.PacketByteBuf
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

/**
 * 区块加载器方块实体。
 * 使用 ChunkTicketType.FORCED 逐区块强制加载，耗能 1 EU/tick 每区块。
 */
@ModBlockEntity(block = ChunkLoaderBlock::class)
class ChunkLoaderBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, ExtendedScreenHandlerFactory {

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

    /** 当前已添加 chunk ticket 的区块位置集合 */
    private var activeChunkPositions: Set<ChunkPos> = emptySet()

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

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.chunk_loader")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ChunkLoaderScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ChunkLoaderSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        // 兼容旧存档：Range 0=1区块, 1=9区块, 2=25区块
        if (nbt.contains("Range")) {
            val range = nbt.getInt("Range").coerceIn(0, 2)
            sync.chunkBitmask = when (range) {
                0 -> 1 shl 12  // 仅中心区块 (row=2, col=2)
                1 -> {
                    var mask = 0
                    for (dy in -1..1) for (dx in -1..1) {
                        mask = mask or (1 shl ((dy + 2) * 5 + (dx + 2)))
                    }
                    mask
                }
                else -> ChunkLoaderSync.DEFAULT_BITMASK  // 5×5 全选
            }
        } else {
            sync.chunkBitmask = nbt.getInt("ChunkBitmask")
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ChunkLoaderSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("ChunkBitmask", sync.chunkBitmask)
    }

    override fun markRemoved() {
        releaseChunks()
        super.markRemoved()
    }

    /** 释放所有强制加载的区块 */
    fun releaseChunks() {
        if (activeChunkPositions.isEmpty()) return
        val sw = world as? ServerWorld ?: return
        val cm = sw.chunkManager as? ServerChunkManager ?: return
        for (chunkPos in activeChunkPositions) {
            cm.removeTicket(ChunkTicketType.FORCED, chunkPos, 0, chunkPos)
        }
        activeChunkPositions = emptySet()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        val chunkCount = sync.getChunkCount()
        val needPerTick = ChunkLoaderSync.EU_PER_CHUNK_PER_TICK * chunkCount

        if (sync.amount < needPerTick || chunkCount == 0) {
            // 能量不足或无区块选中，停止加载
            if (activeChunkPositions.isNotEmpty()) {
                releaseChunks()
                setActiveState(world, pos, state, false)
            }
            sync.syncCurrentTickFlow()
            return
        }

        // 消耗能量并维持加载
        if (sync.consumeEnergy(needPerTick) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            val center = ChunkPos(pos)
            val needed = sync.getChunkPositions(center).toSet()

            val sw = world as? ServerWorld
            val cm = sw?.chunkManager as? ServerChunkManager
            if (cm != null) {
                // 新增需要的区块 ticket
                for (cp in needed - activeChunkPositions) {
                    cm.addTicket(ChunkTicketType.FORCED, cp, 0, cp)
                }
                // 移除不再需要的区块 ticket
                for (cp in activeChunkPositions - needed) {
                    cm.removeTicket(ChunkTicketType.FORCED, cp, 0, cp)
                }
                activeChunkPositions = needed
            }
            setActiveState(world, pos, state, true)
            markDirty()
        } else {
            if (activeChunkPositions.isNotEmpty()) releaseChunks()
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    /** 切换指定区块的加载状态（由 ScreenHandler 调用），中心区块不可关闭 */
    fun toggleChunk(index: Int) {
        if (index in 0 until ChunkLoaderSync.CHUNK_COUNT && index != 12) {
            sync.chunkBitmask = sync.chunkBitmask xor (1 shl index)
            markDirty()
        }
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

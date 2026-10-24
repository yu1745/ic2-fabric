package ic2_120.content.block.machines

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.content.block.ChunkLoaderBlock
import ic2_120.content.block.ITieredMachine
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.ChunkLoaderScreenHandler
import ic2_120.content.sync.ChunkLoaderSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerChunkCache
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.server.level.TicketType

/**
 * 区块加载器的强制加载票据有意仅在本 BlockEntity tick 时创建，不做服务器重启后的持久化恢复。
 * 因此服务器重启后，如果加载器所在区块没有被玩家、出生区、/forceload 或其他加载源加载，
 * 本 BlockEntity 不会自行唤醒并重新添加票据；这是为避免建立独立的全局持久化与启动恢复机制而作出的设计取舍。
 */
@ModBlockEntity(block = ChunkLoaderBlock::class)
class ChunkLoaderBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), ITieredMachine, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = ChunkLoaderBlock.ACTIVE
    override fun getInventory(): net.minecraft.world.Container? = null
    override val tier: Int = 1

    val syncedData = SyncedData()
    @RegisterEnergy
    val sync = ChunkLoaderSync(syncedData) { world?.time }

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private var activeChunkPositions: Set<ChunkPos> = emptySet()

    constructor(pos: BlockPos, state: BlockState) : this(
        ChunkLoaderBlockEntity::class.type(), pos, state
    )

    override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.chunk_loader")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        ChunkLoaderScreenHandler(syncId, playerInventory, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData)

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ChunkLoaderSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        // 兼容旧存档：Range 0=1区块, 1=9区块, 2=25区块  或旧 5×5 bitmask
        if (nbt.contains("Range")) {
            val range = nbt.getInt("Range").coerceIn(0, 2)
            val oldHalf = 2
            val newHalf = ChunkLoaderSync.GRID_SIZE / 2
            sync.chunkBitmask0 = 0
            sync.chunkBitmask1 = 0
            sync.chunkBitmask2 = 0
            when (range) {
                0 -> {
                    val idx = newHalf * ChunkLoaderSync.GRID_SIZE + newHalf
                    sync.toggleChunkBit(idx)
                }
                1 -> {
                    for (dy in -1..1) for (dx in -1..1) {
                        val idx = (newHalf + dy) * ChunkLoaderSync.GRID_SIZE + (newHalf + dx)
                        sync.toggleChunkBit(idx)
                    }
                }
                else -> {
                    for (dy in -oldHalf..oldHalf) for (dx in -oldHalf..oldHalf) {
                        val idx = (newHalf + dy) * ChunkLoaderSync.GRID_SIZE + (newHalf + dx)
                        sync.toggleChunkBit(idx)
                    }
                }
            }
        } else if (nbt.contains("ChunkBitmask")) {
            val oldMask = nbt.getInt("ChunkBitmask")
            val newHalf = ChunkLoaderSync.GRID_SIZE / 2
            sync.chunkBitmask0 = 0
            sync.chunkBitmask1 = 0
            sync.chunkBitmask2 = 0
            for (i in 0 until 25) {
                if ((oldMask and (1 shl i)) != 0) {
                    val oldRow = i / 5
                    val oldCol = i % 5
                    val newIdx = (newHalf + oldRow - 2) * ChunkLoaderSync.GRID_SIZE + (newHalf + oldCol - 2)
                    sync.toggleChunkBit(newIdx)
                }
            }
        } else {
            sync.chunkBitmask0 = nbt.getInt("ChunkBitmask0")
            sync.chunkBitmask1 = nbt.getInt("ChunkBitmask1")
            sync.chunkBitmask2 = nbt.getInt("ChunkBitmask2")
        }
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(ChunkLoaderSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt("ChunkBitmask0", sync.chunkBitmask0)
        nbt.putInt("ChunkBitmask1", sync.chunkBitmask1)
        nbt.putInt("ChunkBitmask2", sync.chunkBitmask2)
    }

    override fun markRemoved() {
        releaseChunks()
        super.markRemoved()
    }

    fun releaseChunks() {
        if (activeChunkPositions.isEmpty()) return
        val sw = world as? ServerLevel ?: return
        val cm = sw.chunkSource ?: return
        for (chunkPos in activeChunkPositions) {
            // ServerChunkCache.addTicket 的 radius=2 对应 ENTITY_TICKING 等级，
            // 确保区块内的方块实体和实体逻辑在玩家离开后仍继续 tick。
            cm.removeTicket(TicketType.FORCED, chunkPos, 2, chunkPos)
        }
        activeChunkPositions = emptySet()
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()

        val chunkCount = sync.getChunkCount()
        val needPerTick = ChunkLoaderSync.EU_PER_CHUNK_PER_TICK * chunkCount

        if (sync.amount < needPerTick || chunkCount == 0) {
            if (activeChunkPositions.isNotEmpty()) {
                releaseChunks()
                setActiveState(world, pos, state, false)
            }
            sync.syncCurrentTickFlow()
            return
        }

        if (sync.consumeEnergy(needPerTick) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            val center = ChunkPos(pos)
            val needed = sync.getChunkPositions(center).toSet()

            val sw = world as? ServerLevel
            val cm = sw?.chunkSource
            if (cm != null) {
                for (cp in needed - activeChunkPositions) {
                    // radius=2 -> ENTITY_TICKING；radius=0 只有 FULL，不会 tick 方块实体。
                    cm.addTicket(TicketType.FORCED, cp, 2, cp)
                }
                for (cp in activeChunkPositions - needed) {
                    cm.removeTicket(TicketType.FORCED, cp, 2, cp)
                }
                activeChunkPositions = needed
            }
            setActiveState(world, pos, state, true)
            setChanged()
        } else {
            if (activeChunkPositions.isNotEmpty()) releaseChunks()
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    fun toggleChunk(index: Int) {
        if (index !in 0 until ChunkLoaderSync.CHUNK_COUNT) return
        // 中心区块恒定加载，不可切换
        if (index == ChunkLoaderSync.CENTER_INDEX) return
        // 已达上限且是开启操作 → 拒绝
        if (!sync.isChunkEnabled(index) && sync.getChunkCount() >= ChunkLoaderSync.MAX_LOADED_CHUNKS) return
        sync.toggleChunkBit(index)
        setChanged()
    }
}

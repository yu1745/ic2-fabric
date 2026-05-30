package ic2_120.content.block.machines

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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerChunkManager
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.registry.RegistryWrapper
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

@ModBlockEntity(block = ChunkLoaderBlock::class)
class ChunkLoaderBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), ITieredMachine, ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = ChunkLoaderBlock.ACTIVE
    override fun getInventory(): net.minecraft.inventory.Inventory? = null
    override val tier: Int = 1

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = ChunkLoaderSync(syncedData) { world?.time }

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    private var activeChunkPositions: Set<ChunkPos> = emptySet()

    constructor(pos: BlockPos, state: BlockState) : this(
        ChunkLoaderBlockEntity::class.type(), pos, state
    )

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.chunk_loader")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ChunkLoaderScreenHandler(syncId, playerInventory, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
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

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
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
        val sw = world as? ServerWorld ?: return
        val cm = sw.chunkManager ?: return
        for (chunkPos in activeChunkPositions) {
            cm.removeTicket(ChunkTicketType.FORCED, chunkPos, 0, chunkPos)
        }
        activeChunkPositions = emptySet()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
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

            val sw = world as? ServerWorld
            val cm = sw?.chunkManager
            if (cm != null) {
                for (cp in needed - activeChunkPositions) {
                    cm.addTicket(ChunkTicketType.FORCED, cp, 0, cp)
                }
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

    fun toggleChunk(index: Int) {
        if (index !in 0 until ChunkLoaderSync.CHUNK_COUNT) return
        // 中心区块恒定加载，不可切换
        if (index == ChunkLoaderSync.CENTER_INDEX) return
        // 已达上限且是开启操作 → 拒绝
        if (!sync.isChunkEnabled(index) && sync.getChunkCount() >= ChunkLoaderSync.MAX_LOADED_CHUNKS) return
        sync.toggleChunkBit(index)
        markDirty()
    }
}

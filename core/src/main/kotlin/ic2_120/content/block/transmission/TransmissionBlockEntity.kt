package ic2_120.content.block.transmission

import ic2_120.compat.LegacyBlockEntity26

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import net.minecraft.core.HolderLookup
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos

/**
 * 传动系统方块实体（仅用于客户端 BER 动画渲染）。
 *
 * 不参与电力系统，不存储额外状态。
 */
@ModBlockEntity(
    name = "transmission",
    blocks = [
        WoodTransmissionShaftBlock::class,
        IronTransmissionShaftBlock::class,
        SteelTransmissionShaftBlock::class,
        CarbonTransmissionShaftBlock::class,
        BevelGearBlock::class
    ]
)
class TransmissionBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : LegacyBlockEntity26(type, pos, state) {

    constructor(pos: BlockPos, state: BlockState) : this(
        TransmissionBlockEntity::class.type(),
        pos,
        state
    )

    var currentKu: Int = 0
        private set

    fun setCurrentKu(value: Int) {
        val clamped = value.coerceAtLeast(0)
        if (currentKu == clamped) return
        currentKu = clamped
        setChanged()
        val world = world as? ServerLevel ?: return
        val state = world.getBlockState(pos)
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)
        world.chunkSource.blockChanged(pos)
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        currentKu = nbt.getIntOr("CurrentKu", 0).coerceAtLeast(0)
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        nbt.putInt("CurrentKu", currentKu)
    }

    override fun getUpdateTag(lookup: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(lookup)

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    fun tick(world: net.minecraft.world.level.Level) {
        if (world.isClient) return
        KineticNetworkManager.tickAt(world, pos)
    }

    companion object {
        val TYPE: BlockEntityType<TransmissionBlockEntity> get() = TransmissionBlockEntity::class.type()
    }
}

package ic2_120.content.block.transmission

import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

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
) : BlockEntity(type, pos, state) {

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
        markDirty()
        val world = world as? ServerWorld ?: return
        val state = world.getBlockState(pos)
        world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
        world.chunkManager.markForUpdate(pos)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        currentKu = nbt.getInt("CurrentKu").coerceAtLeast(0)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putInt("CurrentKu", currentKu)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    fun tick(world: net.minecraft.world.World) {
        if (world.isClient) return
        KineticNetworkManager.tickAt(world, pos)
    }

    companion object {
        val TYPE: BlockEntityType<TransmissionBlockEntity> get() = TransmissionBlockEntity::class.type()
    }
}

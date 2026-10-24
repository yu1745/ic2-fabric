package ic2_120.content.block.pipes

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.cachedState
import ic2_120.compat.pos
import ic2_120.compat.world

import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluid
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level

@ModBlockEntity(
    name = "pipe",
    blocks = [
        BronzePipeTinyBlock::class,
        BronzePipeSmallBlock::class,
        BronzePipeMediumBlock::class,
        BronzePipeLargeBlock::class,
        CarbonPipeTinyBlock::class,
        CarbonPipeSmallBlock::class,
        CarbonPipeMediumBlock::class,
        CarbonPipeLargeBlock::class,
        BronzePumpAttachmentBlock::class,
        CarbonPumpAttachmentBlock::class
    ]
)
class PipeBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(PipeBlockEntity::class.type(), pos, state), ExtendedScreenHandlerFactory<FriendlyByteBuf> {
    var network: PipeNetwork? = null
    var pipeLoad: Long = 0L
    var currentFluidId: String? = null
    private var disabledMask: Int = 0
    private var pumpFilterFluidId: String? = null

    fun isDisabled(direction: Direction): Boolean {
        val bit = 1 shl direction.get3DDataValue()
        return (disabledMask and bit) != 0
    }

    fun toggleDisabled(direction: Direction): Boolean {
        val bit = 1 shl direction.get3DDataValue()
        disabledMask = if ((disabledMask and bit) != 0) disabledMask and bit.inv() else disabledMask or bit
        setChanged()
        return (disabledMask and bit) != 0
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        // 网络管理（创建/关联）在 end server tick 统一推动 fluid 传输
        if (network == null) network = PipeNetworkManager.getOrCreateNetwork(world, pos)
    }

    fun pumpFilterFluid(): Fluid? {
        val raw = pumpFilterFluidId ?: return null
        val id = ResourceLocation.tryParse(raw) ?: return null
        return if (BuiltInRegistries.FLUID.containsKey(id)) BuiltInRegistries.FLUID.get(id) else null
    }

    fun isPumpAttachment(): Boolean = cachedState.block is PumpAttachmentBlock

    fun setPumpFilterFluid(fluid: Fluid) {
        pumpFilterFluidId = BuiltInRegistries.FLUID.getKey(fluid).toString()
        setChanged()
        world?.sendBlockUpdated(pos, cachedState, cachedState, Block.UPDATE_CLIENTS)
    }

    fun clearPumpFilter() {
        pumpFilterFluidId = null
        setChanged()
        world?.sendBlockUpdated(pos, cachedState, cachedState, Block.UPDATE_CLIENTS)
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        disabledMask = nbt.getInt("DisabledMask")
        pipeLoad = nbt.getLong("PipeLoad")
        currentFluidId = nbt.getString("CurrentFluid").takeIf { it.isNotBlank() }
        pumpFilterFluidId = nbt.getString("PumpFilterFluid").takeIf { it.isNotBlank() }
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        nbt.putInt("DisabledMask", disabledMask)
        nbt.putLong("PipeLoad", pipeLoad)
        currentFluidId?.let { nbt.putString("CurrentFluid", it) }
        if (!pumpFilterFluidId.isNullOrBlank()) {
            nbt.putString("PumpFilterFluid", pumpFilterFluidId)
        }
    }

    override fun getUpdateTag(lookup: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(lookup)

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.pump_attachment")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        ic2_120.content.screen.PumpAttachmentScreenHandler(syncId, playerInventory, this)
}

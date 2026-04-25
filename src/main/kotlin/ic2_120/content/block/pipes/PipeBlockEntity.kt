package ic2_120.content.block.pipes

import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.text.Text
import net.minecraft.world.World
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

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
class PipeBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(PipeBlockEntity::class.type(), pos, state), ExtendedScreenHandlerFactory<PacketByteBuf> {
    var network: PipeNetwork? = null
    var pipeLoad: Long = 0L
    var currentFluidId: String? = null
    private var disabledMask: Int = 0
    private var pumpFilterGhostStack: ItemStack = ItemStack.EMPTY
    private var pumpFilterFluidId: String? = null

    fun isDisabled(direction: Direction): Boolean {
        val bit = 1 shl direction.id
        return (disabledMask and bit) != 0
    }

    fun toggleDisabled(direction: Direction): Boolean {
        val bit = 1 shl direction.id
        disabledMask = if ((disabledMask and bit) != 0) disabledMask and bit.inv() else disabledMask or bit
        markDirty()
        return (disabledMask and bit) != 0
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        val net = network ?: PipeNetworkManager.getOrCreateNetwork(world, pos).also { network = it }
        net.tickIfNeeded(world)
    }

    fun pumpFilterFluid(): Fluid? {
        val raw = pumpFilterFluidId ?: return null
        val id = Identifier.tryParse(raw) ?: return null
        return if (Registries.FLUID.containsId(id)) Registries.FLUID.get(id) else null
    }

    fun isPumpAttachment(): Boolean = cachedState.block is PumpAttachmentBlock

    fun pumpFilterGhostStack(): ItemStack = pumpFilterGhostStack

    fun setPumpFilterFromStack(stack: ItemStack): Boolean {
        val fluidId = PumpAttachmentFilter.resolveFluidIdFromStack(stack) ?: return false
        pumpFilterFluidId = fluidId
        pumpFilterGhostStack = stack.copy().apply { count = 1 }
        markDirty()
        return true
    }

    fun clearPumpFilter() {
        pumpFilterFluidId = null
        pumpFilterGhostStack = ItemStack.EMPTY
        markDirty()
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        disabledMask = nbt.getInt("DisabledMask")
        pipeLoad = nbt.getLong("PipeLoad")
        currentFluidId = nbt.getString("CurrentFluid").takeIf { it.isNotBlank() }
        if (nbt.contains("PumpFilterSlot")) {
            pumpFilterGhostStack = ItemStack.fromNbt(lookup, nbt.getCompound("PumpFilterSlot")).orElse(ItemStack.EMPTY)
        } else {
            pumpFilterGhostStack = ItemStack.EMPTY
        }
        pumpFilterFluidId = nbt.getString("PumpFilterFluid").takeIf { it.isNotBlank() }
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putInt("DisabledMask", disabledMask)
        nbt.putLong("PipeLoad", pipeLoad)
        currentFluidId?.let { nbt.putString("CurrentFluid", it) }
        if (!pumpFilterGhostStack.isEmpty) {
            nbt.put("PumpFilterSlot", pumpFilterGhostStack.encode(lookup))
        }
        if (!pumpFilterFluidId.isNullOrBlank()) {
            nbt.putString("PumpFilterFluid", pumpFilterFluidId)
        }
    }

    override fun toInitialChunkDataNbt(lookup: RegistryWrapper.WrapperLookup): NbtCompound = createNbt(lookup)

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    override fun getScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.pump_attachment")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ic2_120.content.screen.PumpAttachmentScreenHandler(syncId, playerInventory, this)
}

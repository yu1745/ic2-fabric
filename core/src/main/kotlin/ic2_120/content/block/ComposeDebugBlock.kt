package ic2_120.content.block

import com.mojang.serialization.MapCodec
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

/**
 * Compose UI 调试方块。
 * 右键打开 ComposeDebugScreen，用于测试 ScrollView 等 Compose UI 组件。
 * 使用 STONE 作为基础（BARRIER 放置后不可见）。
 */
@ModBlock(
    name = "compose_debug",
    registerItem = true,
    tab = CreativeTab.IC2_MACHINES,
    group = "misc"
)
class ComposeDebugBlock : BlockWithEntity(
    AbstractBlock.Settings.copy(Blocks.STONE).strength(-1.0f, 6000000.0f)
) {
    companion object {
        val DEBUG_CODEC: MapCodec<ComposeDebugBlock> = Block.createCodec { error("ComposeDebugBlock cannot be deserialized from JSON") }
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> = DEBUG_CODEC

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return ComposeDebugBlockEntity(pos, state)
    }
    override fun getRenderType(state: BlockState): BlockRenderType {
        return BlockRenderType.MODEL
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun getName(): MutableText = Text.translatable("block.ic2_120.compose_debug")

}

@ModBlockEntity(block = ComposeDebugBlock::class)
class ComposeDebugBlockEntity(
    type: BlockEntityType<ComposeDebugBlockEntity>,
    pos: net.minecraft.util.math.BlockPos,
    state: net.minecraft.block.BlockState
) : BlockEntity(type, pos, state),
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<PacketByteBuf> {

    constructor(pos: net.minecraft.util.math.BlockPos, state: net.minecraft.block.BlockState) : this(
        ComposeDebugBlockEntity::class.type(), pos, state
    )

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.compose_debug")

    override fun createMenu(
        syncId: Int,
        playerInventory: net.minecraft.entity.player.PlayerInventory,
        player: net.minecraft.entity.player.PlayerEntity?
    ): net.minecraft.screen.ScreenHandler =
        ic2_120.content.screen.ComposeDebugScreenHandler(syncId, playerInventory)

    override fun getScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        return buf
    }
}

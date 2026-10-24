package ic2_120.content.block

import com.mojang.serialization.MapCodec
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.FriendlyByteBuf
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
class ComposeDebugBlock : BaseEntityBlock(
    BlockBehaviour.Settings.copy(Blocks.STONE).strength(-1.0f, 6000000.0f)
) {
    companion object {
        val DEBUG_CODEC: MapCodec<ComposeDebugBlock> = Block.createCodec { error("ComposeDebugBlock cannot be deserialized from JSON") }
    }

    override fun getCodec(): MapCodec<out BaseEntityBlock> = DEBUG_CODEC

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return ComposeDebugBlockEntity(pos, state)
    }
    override fun getRenderType(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun getName(): MutableComponent = Component.translatable("block.ic2_120.compose_debug")

}

@ModBlockEntity(block = ComposeDebugBlock::class)
class ComposeDebugBlockEntity(
    type: BlockEntityType<ComposeDebugBlockEntity>,
    pos: net.minecraft.core.BlockPos,
    state: net.minecraft.world.level.block.state.BlockState
) : BlockEntity(type, pos, state),
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    constructor(pos: net.minecraft.core.BlockPos, state: net.minecraft.world.level.block.state.BlockState) : this(
        ComposeDebugBlockEntity::class.type(), pos, state
    )

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.compose_debug")

    override fun createMenu(
        syncId: Int,
        playerInventory: net.minecraft.world.entity.player.Container,
        player: net.minecraft.world.entity.player.Player?
    ): net.minecraft.world.inventory.AbstractContainerMenu =
        ic2_120.content.screen.ComposeDebugScreenHandler(syncId, playerInventory)

    override fun getScreenOpeningData(player: net.minecraft.server.level.ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        return buf
    }
}

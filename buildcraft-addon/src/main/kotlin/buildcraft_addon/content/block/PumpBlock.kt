package buildcraft_addon.content.block

import ic2_120.compat.displayClientMessage

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PumpBlockEntity
import ic2_120.integration.ftbchunks.ClaimProtection
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ic2_120.compat.*
import net.minecraft.world.level.block.state.BlockBehaviour

@ModBlock(name = "pump", registerItem = true, tab = CreativeTab.BUILDCRAFT)
class PumpBlock(properties: BlockBehaviour.Properties = Properties.of().strength(5.0f, 10.0f).noOcclusion()) : BaseEntityBlock(properties), EntityBlock {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec(::PumpBlock)


    init {
        registerDefaultState(defaultBlockState())
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {}

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return PumpBlockEntity(pos, state)
    }

    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? PumpBlockEntity ?: return InteractionResult.PASS
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("液泵: ${be.getTankInfo()}"), true)
        }
        return InteractionResult.SUCCESS
    }

    override fun neighborChanged(state: BlockState, world: Level, pos: BlockPos, sourceBlock: Block, orientation: net.minecraft.world.level.redstone.Orientation?, notify: Boolean) {
        super.neighborChanged(state, world, pos, sourceBlock, orientation, notify)
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? PumpBlockEntity ?: return
            be.checkRedstonePower()
        }
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (world.isClient) {
            BlockEntityTicker { _, _, _, be -> (be as PumpBlockEntity).clientTick(1) }
        } else {
            BlockEntityTicker { _, _, _, be -> (be as PumpBlockEntity).serverTick() }
        }
    }

    override fun getCloneItemStack(world: net.minecraft.world.level.LevelReader, pos: BlockPos, state: BlockState, includeData: Boolean): ItemStack {
        return ItemStack(this)
    }

}

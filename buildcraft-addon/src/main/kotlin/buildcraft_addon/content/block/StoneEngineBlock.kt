package buildcraft_addon.content.block

import buildcraft_addon.content.blockentity.StoneEngineBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.MenuProvider

@ModBlock(name = "stone_engine", registerItem = true, tab = CreativeTab.BUILDCRAFT)
class StoneEngineBlock : BaseEntityBlock(
    Settings.create().strength(3.0f, 5.0f).nonOpaque()
), EntityBlock {

    companion object {
        private val SHAPE_ENGINE: VoxelShape = Shapes.fullCube()
    }

    init {
        defaultState = stateManager.defaultState.with(BlockStateProperties.FACING, Direction.UP)
    }

    override fun appendProperties(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.FACING)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.with(BlockStateProperties.FACING, rotation.rotate(state.get(BlockStateProperties.FACING)))
    }

    override fun onUse(
        state: BlockState, world: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? StoneEngineBlockEntity ?: return InteractionResult.PASS
            be.checkRedstonePower()
            if (be is MenuProvider) {
                player.openHandledScreen(be)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun onPlaced(world: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? StoneEngineBlockEntity ?: return
            be.currentDirection = state.get(BlockStateProperties.FACING)
            be.markDirty()
        }
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState {
        return defaultState.with(BlockStateProperties.FACING, ctx.side.opposite)
    }

    override fun neighborUpdate(state: BlockState, world: Level, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos, notify: Boolean) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
        val be = world.getBlockEntity(pos) as? StoneEngineBlockEntity ?: return
        be.checkRedstonePower()
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return StoneEngineBlockEntity(pos, state)
    }

    override fun getRenderType(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun getOutlineShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: net.minecraft.world.phys.shapes.CollisionContext): VoxelShape {
        return SHAPE_ENGINE
    }

    override fun getCollisionShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: net.minecraft.world.phys.shapes.CollisionContext): VoxelShape {
        return SHAPE_ENGINE
    }

    override fun getCullingShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape {
        return Shapes.empty()
    }

    override fun isTransparent(state: BlockState, world: BlockGetter, pos: BlockPos): Boolean {
        return true
    }

    override fun hasSidedTransparency(state: BlockState): Boolean {
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (world.isClient) {
            BlockEntityTicker { _, _, _, be -> (be as StoneEngineBlockEntity).clientTick() }
        } else {
            BlockEntityTicker { _, _, _, be -> (be as StoneEngineBlockEntity).serverTick() }
        }
    }
}

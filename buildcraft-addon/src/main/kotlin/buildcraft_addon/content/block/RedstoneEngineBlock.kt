package buildcraft_addon.content.block

import buildcraft_addon.content.blockentity.RedstoneEngineBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.BlockRotation
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

@ModBlock(name = "redstone_engine", registerItem = true, tab = CreativeTab.BUILDCRAFT)
class RedstoneEngineBlock : BlockWithEntity(
    Settings.create().strength(2.0f, 3.0f).nonOpaque()
), BlockEntityProvider {

    companion object {
        private val SHAPE_ENGINE: VoxelShape = VoxelShapes.fullCube()
    }

    init {
        defaultState = stateManager.defaultState.with(Properties.FACING, Direction.UP)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.FACING)
    }

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState {
        return state.with(Properties.FACING, rotation.rotate(state.get(Properties.FACING)))
    }

    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hand: Hand, hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? RedstoneEngineBlockEntity ?: return ActionResult.PASS
            be.rotateToNextFacing(state.get(Properties.FACING))
        }
        return ActionResult.SUCCESS
    }

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? RedstoneEngineBlockEntity ?: return
            be.currentDirection = state.get(Properties.FACING)
            be.markDirty()
        }
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        return defaultState.with(Properties.FACING, ctx.side.opposite)
    }

    override fun neighborUpdate(state: BlockState, world: World, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos, notify: Boolean) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
        val be = world.getBlockEntity(pos) as? RedstoneEngineBlockEntity ?: return
        be.checkRedstonePower()
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RedstoneEngineBlockEntity(pos, state)
    }

    override fun getRenderType(state: BlockState): BlockRenderType {
        return BlockRenderType.ENTITYBLOCK_ANIMATED
    }

    override fun getOutlineShape(state: BlockState, world: BlockView, pos: BlockPos, context: net.minecraft.block.ShapeContext): VoxelShape {
        return SHAPE_ENGINE
    }

    override fun getCollisionShape(state: BlockState, world: BlockView, pos: BlockPos, context: net.minecraft.block.ShapeContext): VoxelShape {
        return SHAPE_ENGINE
    }

    override fun getCullingShape(state: BlockState, world: BlockView, pos: BlockPos): VoxelShape {
        return VoxelShapes.empty()
    }

    override fun isTransparent(state: BlockState, world: BlockView, pos: BlockPos): Boolean {
        return true
    }

    override fun hasSidedTransparency(state: BlockState): Boolean {
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (world.isClient) {
            BlockEntityTicker { _, _, _, be -> (be as RedstoneEngineBlockEntity).clientTick() }
        } else {
            BlockEntityTicker { _, _, _, be -> (be as RedstoneEngineBlockEntity).serverTick() }
        }
    }
}

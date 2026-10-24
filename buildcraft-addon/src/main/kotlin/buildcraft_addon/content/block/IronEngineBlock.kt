package buildcraft_addon.content.block

import buildcraft_addon.content.blockentity.IronEngineBlockEntity
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
import ic2_120.compat.*
import net.minecraft.world.level.block.state.BlockBehaviour

@ModBlock(name = "iron_engine", registerItem = true, tab = CreativeTab.BUILDCRAFT)
class IronEngineBlock(properties: BlockBehaviour.Properties = Properties.of().strength(5.0f, 10.0f).noOcclusion()) : BaseEntityBlock(properties), EntityBlock {
    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> = simpleCodec(::IronEngineBlock)


    companion object {
        private val SHAPE_ENGINE: VoxelShape = Shapes.block()
    }

    init {
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.FACING)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)))
    }

    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? IronEngineBlockEntity ?: return InteractionResult.PASS
            be.checkRedstonePower()
            if (be is MenuProvider) {
                player.openMenu(be)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun setPlacedBy(world: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? IronEngineBlockEntity ?: return
            be.currentDirection = state.getValue(BlockStateProperties.FACING)
            be.setChanged()
        }
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(BlockStateProperties.FACING, ctx.clickedFace.opposite)
    }

    override fun neighborChanged(state: BlockState, world: Level, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos, notify: Boolean) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify)
        val be = world.getBlockEntity(pos) as? IronEngineBlockEntity ?: return
        be.checkRedstonePower()
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return IronEngineBlockEntity(pos, state)
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: net.minecraft.world.phys.shapes.CollisionContext): VoxelShape {
        return SHAPE_ENGINE
    }

    override fun getCollisionShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: net.minecraft.world.phys.shapes.CollisionContext): VoxelShape {
        return SHAPE_ENGINE
    }




    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (world.isClient) {
            BlockEntityTicker { _, _, _, be -> (be as IronEngineBlockEntity).clientTick() }
        } else {
            BlockEntityTicker { _, _, _, be -> (be as IronEngineBlockEntity).serverTick() }
        }
    }
}

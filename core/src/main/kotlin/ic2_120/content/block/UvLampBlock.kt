package ic2_120.content.block

import com.mojang.serialization.MapCodec

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.UvLampBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.Containers
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

/**
 * 紫外线灯方块。扁平面板外形（与日光灯相同），支持六面朝向。
 * 有电时提供照明（light level 15），放置超频升级后可加速半径 3 内 IC2 作物生长。
 * 右键打开 GUI。
 */
@ModBlock(name = "uv_lamp", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class UvLampBlock : BaseEntityBlock(
    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
        .strength(5.0f, 6.0f)
        .lightLevel { state -> if (state.getValue(ACTIVE)) 15 else 0 }
) {
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(BlockStateProperties.FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(BlockStateProperties.FACING, ACTIVE)
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { UvLampBlock() }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val side = ctx.clickedFace
        val facing = if (side.axis == Direction.Axis.Y) side else side.opposite
        return defaultBlockState().setValue(BlockStateProperties.FACING, facing)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        UvLampBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, UvLampBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = lampShape(state.getValue(BlockStateProperties.FACING))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = lampShape(state.getValue(BlockStateProperties.FACING))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { player.openMenu(it) }
        }
        return InteractionResult.SUCCESS
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!world.isClient && !state.`is`(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Container) {
                Containers.dropContents(world, pos, be)
            }
        }
        super.onRemove(state, world, pos, newState, moved)
    }

    private fun lampShape(facing: Direction): VoxelShape = when (facing) {
        Direction.NORTH -> SHAPE_NORTH
        Direction.SOUTH -> SHAPE_SOUTH
        Direction.EAST -> SHAPE_EAST
        Direction.WEST -> SHAPE_WEST
        Direction.UP -> SHAPE_UP
        Direction.DOWN -> SHAPE_DOWN
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        private const val THICK = 1.0 / 16.0

        private val SHAPE_NORTH = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, THICK)
        private val SHAPE_SOUTH = Shapes.box(0.0, 0.0, 1.0 - THICK, 1.0, 1.0, 1.0)
        private val SHAPE_EAST = Shapes.box(1.0 - THICK, 0.0, 0.0, 1.0, 1.0, 1.0)
        private val SHAPE_WEST = Shapes.box(0.0, 0.0, 0.0, THICK, 1.0, 1.0)
        private val SHAPE_UP = Shapes.box(0.0, 0.0, 0.0, 1.0, THICK, 1.0)
        private val SHAPE_DOWN = Shapes.box(0.0, 1.0 - THICK, 0.0, 1.0, 1.0, 1.0)

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val luminator = LuminatorFlatBlock::class.item()
            if (luminator != Items.AIR) {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, UvLampBlock::class.item(), 1)
                    .requires(luminator)
                    .requires(Items.PURPLE_DYE)
                    .unlockedBy(getHasName(luminator), has(luminator))
                    .save(exporter, UvLampBlock::class.id())
            }
        }
    }
}

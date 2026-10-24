package ic2_120.content.block

import ic2_120.content.block.machines.UvLampBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
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
import java.util.function.Consumer

/**
 * 紫外线灯方块。扁平面板外形（与日光灯相同），支持六面朝向。
 * 有电时提供照明（light level 15），放置超频升级后可加速半径 3 内 IC2 作物生长。
 * 右键打开 GUI。
 */
@ModBlock(name = "uv_lamp", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class UvLampBlock : BaseEntityBlock(
    BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK)
        .strength(5.0f, 6.0f)
        .luminance { state -> if (state.get(ACTIVE)) 15 else 0 }
) {
    init {
        defaultState = stateManager.defaultState
            .with(BlockStateProperties.FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateDefinition.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(BlockStateProperties.FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? {
        val side = ctx.side
        val facing = if (side.axis == Direction.Axis.Y) side else side.opposite
        return defaultState.with(BlockStateProperties.FACING, facing)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        UvLampBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, UvLampBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun getRenderType(state: BlockState): RenderShape = RenderShape.MODEL

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = lampShape(state.get(BlockStateProperties.FACING))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = lampShape(state.get(BlockStateProperties.FACING))

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: Level,
        pos: BlockPos
    ): net.minecraft.world.MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.world.MenuProvider
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return InteractionResult.SUCCESS
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Container) {
                Containers.spawn(world, pos, be)
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
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
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        private const val THICK = 1.0 / 16.0

        private val SHAPE_NORTH = Shapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, THICK)
        private val SHAPE_SOUTH = Shapes.cuboid(0.0, 0.0, 1.0 - THICK, 1.0, 1.0, 1.0)
        private val SHAPE_EAST = Shapes.cuboid(1.0 - THICK, 0.0, 0.0, 1.0, 1.0, 1.0)
        private val SHAPE_WEST = Shapes.cuboid(0.0, 0.0, 0.0, THICK, 1.0, 1.0)
        private val SHAPE_UP = Shapes.cuboid(0.0, 0.0, 0.0, 1.0, THICK, 1.0)
        private val SHAPE_DOWN = Shapes.cuboid(0.0, 1.0 - THICK, 0.0, 1.0, 1.0, 1.0)

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val luminator = LuminatorFlatBlock::class.item()
            if (luminator != Items.AIR) {
                ShapelessRecipeBuilder.create(RecipeCategory.MISC, UvLampBlock::class.item(), 1)
                    .input(luminator)
                    .input(Items.PURPLE_DYE)
                    .criterion(hasItem(luminator), conditionsFromItem(luminator))
                    .offerTo(exporter, UvLampBlock::class.id())
            }
        }
    }
}

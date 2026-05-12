package ic2_120.content.block

import com.mojang.serialization.MapCodec
import ic2_120.content.block.machines.UvLampBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.ShapeContext
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemActionResult
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

/**
 * 紫外线灯方块。扁平面板外形（与日光灯相同），支持六面朝向。
 * 有电时提供照明（light level 15），放置超频升级后可加速半径 3 内 IC2 作物生长。
 * 右键打开 GUI。
 */
@ModBlock(name = "uv_lamp", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class UvLampBlock : BlockWithEntity(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
        .strength(5.0f, 6.0f)
        .luminance { state -> if (state.get(ACTIVE)) 15 else 0 }
) {
    init {
        defaultState = stateManager.defaultState
            .with(Properties.FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> = UV_LAMP_CODEC

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val side = ctx.side
        val facing = if (side.axis == Direction.Axis.Y) side else side.opposite
        return defaultState.with(Properties.FACING, facing)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        UvLampBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, UvLampBlockEntity::class.type()) { w, p, s, be ->
            (be as UvLampBlockEntity).tick(w, p, s)
        }

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = lampShape(state.get(Properties.FACING))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = lampShape(state.get(Properties.FACING))

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: World,
        pos: BlockPos
    ): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return ActionResult.SUCCESS
    }

    override fun onUseWithItem(
        stack: ItemStack,
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ItemActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return ItemActionResult.SUCCESS
    }

    override fun onStateReplaced(
        state: BlockState,
        world: World,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Inventory) {
                ItemScatterer.spawn(world, pos, be)
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
        val UV_LAMP_CODEC: MapCodec<UvLampBlock> = Block.createCodec { error("UvLampBlock cannot be deserialized from JSON") }
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        private const val THICK = 1.0 / 16.0

        private val SHAPE_NORTH = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, THICK)
        private val SHAPE_SOUTH = VoxelShapes.cuboid(0.0, 0.0, 1.0 - THICK, 1.0, 1.0, 1.0)
        private val SHAPE_EAST = VoxelShapes.cuboid(1.0 - THICK, 0.0, 0.0, 1.0, 1.0, 1.0)
        private val SHAPE_WEST = VoxelShapes.cuboid(0.0, 0.0, 0.0, THICK, 1.0, 1.0)
        private val SHAPE_UP = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, THICK, 1.0)
        private val SHAPE_DOWN = VoxelShapes.cuboid(0.0, 1.0 - THICK, 0.0, 1.0, 1.0, 1.0)

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val luminator = LuminatorFlatBlock::class.item()
            if (luminator != Items.AIR) {
                ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, UvLampBlock::class.item(), 1)
                    .input(luminator)
                    .input(Items.PURPLE_DYE)
                    .criterion(hasItem(luminator), conditionsFromItem(luminator))
                    .offerTo(exporter, UvLampBlock::class.id())
            }
        }
    }
}

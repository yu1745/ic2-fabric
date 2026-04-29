package ic2_120.content.block

import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.block.cables.TinCableBlock
import ic2_120.content.block.machines.LuminatorFlatBlockEntity
import ic2_120.content.item.IronCasing
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.ShapeContext
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 日光灯方块。支持 6 方向 facing 与 active 状态。
 * 电压等级 5（不限制电压），active 时发光等级 15。
 */
@ModBlock(name = "luminator_flat", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class LuminatorFlatBlock : BlockWithEntity(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
        .strength(5.0f, 6.0f)
        .luminance { state -> if (state.get(ACTIVE)) 15 else 0 }
) {
    override fun getCodec(): MapCodec<out BlockWithEntity> = LUMINATOR_CODEC

    init {
        defaultState = stateManager.defaultState
            .with(Properties.FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val side = ctx.side
        // 侧面：发光朝向房间内部，用 opposite；顶底：发光朝向点击面，用 side（与 blockstate 模型旋转对应）
        val facing = if (side.axis == Direction.Axis.Y) side else side.opposite
        return defaultState.with(Properties.FACING, facing)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        LuminatorFlatBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, LuminatorFlatBlockEntity::class.type()){ w, p, s, be ->
            (be as LuminatorFlatBlockEntity).tick(w, p, s)
        }

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = luminatorShape(state.get(Properties.FACING))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = luminatorShape(state.get(Properties.FACING))

    /** 薄板厚度 1/16，与 luminator_shape_flat 模型一致 */
    private fun luminatorShape(facing: Direction): VoxelShape = when (facing) {
        Direction.NORTH -> SHAPE_NORTH
        Direction.SOUTH -> SHAPE_SOUTH
        Direction.EAST -> SHAPE_EAST
        Direction.WEST -> SHAPE_WEST
        Direction.UP -> SHAPE_UP
        Direction.DOWN -> SHAPE_DOWN
    }

    companion object {
        val LUMINATOR_CODEC: MapCodec<LuminatorFlatBlock> = Block.createCodec { error("LuminatorFlatBlock cannot be deserialized from JSON") }

        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        /** 薄板厚度 1/16，与 luminator_shape_flat 模型一致 */
        private const val THICK = 1.0 / 16.0

        private val SHAPE_NORTH = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, THICK)
        private val SHAPE_SOUTH = VoxelShapes.cuboid(0.0, 0.0, 1.0 - THICK, 1.0, 1.0, 1.0)
        private val SHAPE_EAST = VoxelShapes.cuboid(0.0, 0.0, 0.0, THICK, 1.0, 1.0)
        private val SHAPE_WEST = VoxelShapes.cuboid(1.0 - THICK, 0.0, 0.0, 1.0, 1.0, 1.0)
        private val SHAPE_UP = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, THICK, 1.0)
        private val SHAPE_DOWN = VoxelShapes.cuboid(0.0, 1.0 - THICK, 0.0, 1.0, 1.0, 1.0)

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val insulatedCopperCable = InsulatedCopperCableBlock::class.item()
            val tinCable = TinCableBlock::class.item()
            val ironCasing = IronCasing::class.instance()
            if (insulatedCopperCable != Items.AIR && tinCable != Items.AIR && ironCasing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LuminatorFlatBlock::class.item(), 8)
                    .pattern("ICI")
                    .pattern("GTG")
                    .pattern("GGG")
                    .input('I', ironCasing)
                    .input('C', insulatedCopperCable)
                    .input('G', Items.GLASS)
                    .input('T', tinCable)
                    .criterion(hasItem(insulatedCopperCable), conditionsFromItem(insulatedCopperCable))
                    .offerTo(exporter, LuminatorFlatBlock::class.id())
            }
        }
    }
}

package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

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
import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 日光灯方块。支持 6 方向 facing 与 active 状态。
 * 电压等级 5（不限制电压），active 时发光等级 15。
 */
@ModBlock(name = "luminator_flat", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class LuminatorFlatBlock : BaseEntityBlock(
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

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val side = ctx.clickedFace
        // 侧面：发光朝向房间内部，用 opposite；顶底：发光朝向点击面，用 side（与 blockstate 模型旋转对应）
        val facing = if (side.axis == Direction.Axis.Y) side else side.opposite
        return defaultBlockState().setValue(BlockStateProperties.FACING, facing)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        LuminatorFlatBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, LuminatorFlatBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { LuminatorFlatBlock() }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = luminatorShape(state.getValue(BlockStateProperties.FACING))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = luminatorShape(state.getValue(BlockStateProperties.FACING))

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
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        /** 薄板厚度 1/16，与 luminator_shape_flat 模型一致 */
        private const val THICK = 1.0 / 16.0

        private val SHAPE_NORTH = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, THICK)
        private val SHAPE_SOUTH = Shapes.box(0.0, 0.0, 1.0 - THICK, 1.0, 1.0, 1.0)
        private val SHAPE_EAST = Shapes.box(1.0 - THICK, 0.0, 0.0, 1.0, 1.0, 1.0)
        private val SHAPE_WEST = Shapes.box(0.0, 0.0, 0.0, THICK, 1.0, 1.0)
        private val SHAPE_UP = Shapes.box(0.0, 0.0, 0.0, 1.0, THICK, 1.0)
        private val SHAPE_DOWN = Shapes.box(0.0, 1.0 - THICK, 0.0, 1.0, 1.0, 1.0)

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val insulatedCopperCable = InsulatedCopperCableBlock::class.item()
            val tinCable = TinCableBlock::class.item()
            val ironCasing = IronCasing::class.instance()
            if (insulatedCopperCable != Items.AIR && tinCable != Items.AIR && ironCasing != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LuminatorFlatBlock::class.item(), 8)
                    .pattern("ICI")
                    .pattern("GTG")
                    .pattern("GGG")
                    .define('I', ironCasing)
                    .define('C', insulatedCopperCable)
                    .define('G', Items.GLASS)
                    .define('T', tinCable)
                    .unlockedBy(getHasName(insulatedCopperCable), has(insulatedCopperCable))
                    .save(exporter, LuminatorFlatBlock::class.id())
            }
        }
    }
}

package ic2_120.content.block

import ic2_120.content.block.machines.CokeKilnBlockEntity
import ic2_120.content.block.machines.CokeKilnGrateBlockEntity
import ic2_120.registry.type
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.BlockRenderType
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.DirectionProperty
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.function.Consumer

@ModBlock(name = "refractory_bricks", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
/** 耐火砖（Refractory Bricks） */
class RefractoryBricksBlock : Block(AbstractBlock.Settings.copy(Blocks.BRICKS).strength(2.0f, 10.0f)) {
    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, RefractoryBricksBlock::class.item(), 8)
                .pattern("BBB")
                .pattern("BCB")
                .pattern("BBB")
                .input('B', Items.BRICKS)
                .input('C', Items.CLAY_BALL)
                .criterion(hasItem(Items.BRICKS), conditionsFromItem(Items.BRICKS))
                .offerTo(exporter, RefractoryBricksBlock::class.id())
        }
    }
}

@ModBlock(name = "coke_kiln", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "steam")
/** 焦炭窑（Coke Kiln） */
class CokeKilnBlock : MachineBlock(AbstractBlock.Settings.copy(Blocks.BRICKS).strength(3.0f, 10.0f)) {
    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CokeKilnBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, CokeKilnBlockEntity::class.type()){ w, p, s, be ->
            (be as CokeKilnBlockEntity).tick(w, p, s)
        }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? net.minecraft.screen.NamedScreenHandlerFactory
            if (be != null) player.openHandledScreen(be)
        }
        return ActionResult.SUCCESS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, CokeKilnBlock::class.item(), 1)
                .pattern("RRR")
                .pattern("R R")
                .pattern("RRR")
                .input('R', refractory)
                .criterion(hasItem(refractory), conditionsFromItem(refractory))
                .offerTo(exporter, CokeKilnBlock::class.id())
        }
    }
}

@ModBlock(name = "coke_kiln_grate", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "steam")
/** 焦炉炉篦（Coke Kiln Grate） */
class CokeKilnGrateBlock : net.minecraft.block.BlockWithEntity(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f, 10.0f)) {
    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CokeKilnGrateBlockEntity(pos, state)

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState = defaultState.with(FACING, Direction.DOWN)

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val storage = FluidStorage.SIDED.find(world, pos, hit.side) ?: FluidStorage.SIDED.find(world, pos, null)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    companion object {
        val FACING: DirectionProperty = DirectionProperty.of("facing")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, CokeKilnGrateBlock::class.item(), 1)
                .pattern("III")
                .pattern("IRI")
                .pattern("III")
                .input('I', Items.IRON_BARS)
                .input('R', refractory)
                .criterion(hasItem(refractory), conditionsFromItem(refractory))
                .offerTo(exporter, CokeKilnGrateBlock::class.id())
        }
    }

    init {
        defaultState = stateManager.defaultState.with(FACING, Direction.DOWN)
    }
}

@ModBlock(name = "coke_kiln_hatch", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "steam")
/** 焦炉窑口（Coke Kiln Hatch） */
class CokeKilnHatchBlock : Block(AbstractBlock.Settings.copy(Blocks.BRICKS).strength(3.0f, 10.0f)) {
    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val ok = isValidCokeKilnStructure(world, pos)
        player.sendMessage(
            if (ok) Text.translatable("message.ic2_120.coke_kiln.structure_ok")
            else Text.translatable("message.ic2_120.coke_kiln.structure_invalid"),
            true
        )
        return ActionResult.SUCCESS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, CokeKilnHatchBlock::class.item(), 1)
                .pattern("RRR")
                .pattern("RIR")
                .pattern("RRR")
                .input('R', refractory)
                .input('I', Items.IRON_TRAPDOOR)
                .criterion(hasItem(refractory), conditionsFromItem(refractory))
                .offerTo(exporter, CokeKilnHatchBlock::class.id())
        }

        private fun isExpectedMiddleLayerPos(offset: BlockPos): Boolean =
            offset.y == -1 && (kotlin.math.abs(offset.x) + kotlin.math.abs(offset.z) == 1)

        fun isValidCokeKilnStructure(world: World, hatchPos: BlockPos): Boolean {
            // 以窑口为顶层中心：y=0 为顶层，y=-1 中层，y=-2 底层
            for (dy in 0 downTo -2) {
                for (dx in -1..1) {
                    for (dz in -1..1) {
                        val p = hatchPos.add(dx, dy, dz)
                        val offset = BlockPos(dx, dy, dz)

                        // 顶层中心必须是窑口
                        if (dy == 0 && dx == 0 && dz == 0) {
                            if (!world.getBlockState(p).isOf(CokeKilnHatchBlock::class.instance())) return false
                            continue
                        }

                        // 中层中心必须是空气
                        if (dy == -1 && dx == 0 && dz == 0) {
                            if (!world.getBlockState(p).isAir) return false
                            continue
                        }

                        // 底层中心必须是炉篦且朝下
                        if (dy == -2 && dx == 0 && dz == 0) {
                            val state = world.getBlockState(p)
                            if (!state.isOf(CokeKilnGrateBlock::class.instance())) return false
                            if (state.getOrEmpty(CokeKilnGrateBlock.FACING).orElse(Direction.DOWN) != Direction.DOWN) return false
                            continue
                        }

                        val state = world.getBlockState(p)
                        if (isExpectedMiddleLayerPos(offset)) {
                            // 中层四边中心：允许一个主体块，其余耐火砖
                            if (!state.isOf(CokeKilnBlock::class.instance()) && !state.isOf(RefractoryBricksBlock::class.instance())) {
                                return false
                            }
                        } else {
                            if (!state.isOf(RefractoryBricksBlock::class.instance())) return false
                        }
                    }
                }
            }

            // 中层四边中心必须且仅有 1 个主体块
            var kilnCount = 0
            val sideCenters = listOf(
                hatchPos.add(1, -1, 0),
                hatchPos.add(-1, -1, 0),
                hatchPos.add(0, -1, 1),
                hatchPos.add(0, -1, -1)
            )
            for (p in sideCenters) {
                if (world.getBlockState(p).isOf(CokeKilnBlock::class.instance())) kilnCount++
            }
            return kilnCount == 1
        }
    }
}

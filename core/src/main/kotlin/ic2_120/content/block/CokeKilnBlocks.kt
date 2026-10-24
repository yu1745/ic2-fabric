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
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import java.util.function.Consumer

@ModBlock(name = "refractory_bricks", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
/** 耐火砖（Refractory Bricks） */
class RefractoryBricksBlock : Block(BlockBehaviour.Settings.copy(Blocks.BRICKS).strength(2.0f, 10.0f)) {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBlockAdded(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.create(RecipeCategory.BUILDING_BLOCKS, RefractoryBricksBlock::class.item(), 8)
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
class CokeKilnBlock : MachineBlock(BlockBehaviour.Settings.copy(Blocks.BRICKS).strength(3.0f, 10.0f)) {
    override fun getCasingDrop() = asItem()

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBlockAdded(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CokeKilnBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, CokeKilnBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
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
            val be = world.getBlockEntity(pos) as? net.minecraft.world.MenuProvider
            if (be != null) player.openHandledScreen(be)
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeBuilder.create(RecipeCategory.DECORATIONS, CokeKilnBlock::class.item(), 1)
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
class CokeKilnGrateBlock : net.minecraft.world.level.block.BaseEntityBlock(BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f, 10.0f)) {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBlockAdded(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CokeKilnGrateBlockEntity(pos, state)

    override fun getRenderType(state: BlockState): RenderShape = RenderShape.MODEL

    override fun appendProperties(builder: StateDefinition.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(FACING)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState = defaultState.with(FACING, Direction.DOWN)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val storage = FluidStorage.SIDED.find(world, pos, hit.side) ?: FluidStorage.SIDED.find(world, pos, null)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    companion object {
        val FACING: DirectionProperty = DirectionProperty.of("facing")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeBuilder.create(RecipeCategory.DECORATIONS, CokeKilnGrateBlock::class.item(), 1)
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
class CokeKilnHatchBlock : Block(BlockBehaviour.Settings.copy(Blocks.BRICKS).strength(3.0f, 10.0f)) {
    override fun getPlacementState(ctx: BlockPlaceContext): BlockState = defaultState.with(FACING, Direction.UP)

    init {
        defaultState = stateManager.defaultState.with(FACING, Direction.UP)
    }

    override fun appendProperties(builder: StateDefinition.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(FACING)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBlockAdded(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onStateReplaced(state, world, pos, newState, moved)
        if (!world.isClient && !state.isOf(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
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
        if (world.isClient) return InteractionResult.SUCCESS
        val ok = isValidCokeKilnStructure(world, pos)
        player.sendMessage(
            if (ok) Component.translatable("message.ic2_120.coke_kiln.structure_ok")
            else Component.translatable("message.ic2_120.coke_kiln.structure_invalid"),
            true
        )
        return InteractionResult.SUCCESS
    }

    companion object {
        val FACING: DirectionProperty = DirectionProperty.of("facing")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeBuilder.create(RecipeCategory.DECORATIONS, CokeKilnHatchBlock::class.item(), 1)
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

        fun isValidCokeKilnStructure(world: Level, hatchPos: BlockPos): Boolean {
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

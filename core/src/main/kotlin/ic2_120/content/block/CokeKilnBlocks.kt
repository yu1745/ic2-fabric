package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.CokeKilnBlockEntity
import ic2_120.content.block.machines.CokeKilnGrateBlockEntity
import ic2_120.registry.type
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
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
class RefractoryBricksBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).strength(2.0f, 10.0f)) {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onPlace(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onRemove(state, world, pos, newState, moved)
        if (!world.isClient && !state.`is`(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, RefractoryBricksBlock::class.item(), 8)
                .pattern("BBB")
                .pattern("BCB")
                .pattern("BBB")
                .define('B', Items.BRICKS)
                .define('C', Items.CLAY_BALL)
                .unlockedBy(getHasName(Items.BRICKS), has(Items.BRICKS))
                .save(exporter, RefractoryBricksBlock::class.id())
        }
    }
}

@ModBlock(name = "coke_kiln", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "steam")
/** 焦炭窑（Coke Kiln） */
class CokeKilnBlock : MachineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).strength(3.0f, 10.0f)) {
    override fun getCasingDrop() = asItem()

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onPlace(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onRemove(state, world, pos, newState, moved)
        if (!world.isClient && !state.`is`(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CokeKilnBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, CokeKilnBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? net.minecraft.world.MenuProvider
            if (be != null) player.openMenu(be)
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, CokeKilnBlock::class.item(), 1)
                .pattern("RRR")
                .pattern("R R")
                .pattern("RRR")
                .define('R', refractory)
                .unlockedBy(getHasName(refractory), has(refractory))
                .save(exporter, CokeKilnBlock::class.id())
        }
    }
}

@ModBlock(name = "coke_kiln_grate", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "steam")
/** 焦炉炉篦（Coke Kiln Grate） */
class CokeKilnGrateBlock : net.minecraft.world.level.block.BaseEntityBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0f, 10.0f)) {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onPlace(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onRemove(state, world, pos, newState, moved)
        if (!world.isClient && !state.`is`(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    override fun codec(): com.mojang.serialization.MapCodec<out net.minecraft.world.level.block.BaseEntityBlock> =
        BlockBehaviour.simpleCodec { CokeKilnGrateBlock() }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CokeKilnGrateBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(FACING)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState = defaultBlockState().setValue(FACING, Direction.DOWN)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val storage = FluidStorage.SIDED.find(world, pos, hit.direction) ?: FluidStorage.SIDED.find(world, pos, null)
        if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, player.usedItemHand)) {
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    companion object {
        val FACING: DirectionProperty = DirectionProperty.create("facing")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, CokeKilnGrateBlock::class.item(), 1)
                .pattern("III")
                .pattern("IRI")
                .pattern("III")
                .define('I', Items.IRON_BARS)
                .define('R', refractory)
                .unlockedBy(getHasName(refractory), has(refractory))
                .save(exporter, CokeKilnGrateBlock::class.id())
        }
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN))
    }
}

@ModBlock(name = "coke_kiln_hatch", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "steam")
/** 焦炉窑口（Coke Kiln Hatch） */
class CokeKilnHatchBlock : Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).strength(3.0f, 10.0f)) {
    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState = defaultBlockState().setValue(FACING, Direction.UP)

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(FACING)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onPlace(state, world, pos, oldState, notify)
        if (!world.isClient) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        super.onRemove(state, world, pos, newState, moved)
        if (!world.isClient && !state.`is`(newState.block)) CokeKilnBlockEntity.markKilnsDirtyAround(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val ok = isValidCokeKilnStructure(world, pos)
        player.displayClientMessage(
            if (ok) Component.translatable("message.ic2_120.coke_kiln.structure_ok")
            else Component.translatable("message.ic2_120.coke_kiln.structure_invalid"),
            true
        )
        return InteractionResult.SUCCESS
    }

    companion object {
        val FACING: DirectionProperty = DirectionProperty.create("facing")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val refractory = RefractoryBricksBlock::class.item()
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, CokeKilnHatchBlock::class.item(), 1)
                .pattern("RRR")
                .pattern("RIR")
                .pattern("RRR")
                .define('R', refractory)
                .define('I', Items.IRON_TRAPDOOR)
                .unlockedBy(getHasName(refractory), has(refractory))
                .save(exporter, CokeKilnHatchBlock::class.id())
        }

        private fun isExpectedMiddleLayerPos(offset: BlockPos): Boolean =
            offset.y == -1 && (kotlin.math.abs(offset.x) + kotlin.math.abs(offset.z) == 1)

        fun isValidCokeKilnStructure(world: Level, hatchPos: BlockPos): Boolean {
            // 以窑口为顶层中心：y=0 为顶层，y=-1 中层，y=-2 底层
            for (dy in 0 downTo -2) {
                for (dx in -1..1) {
                    for (dz in -1..1) {
                        val p = BlockPos(hatchPos.x + dx, hatchPos.y + dy, hatchPos.z + dz)
                        val offset = BlockPos(dx, dy, dz)

                        // 顶层中心必须是窑口
                        if (dy == 0 && dx == 0 && dz == 0) {
                            if (!world.getBlockState(p).`is`(CokeKilnHatchBlock::class.instance())) return false
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
                            if (!state.`is`(CokeKilnGrateBlock::class.instance())) return false
                            if (state.getOptionalValue(CokeKilnGrateBlock.FACING).orElse(Direction.DOWN) != Direction.DOWN) return false
                            continue
                        }

                        val state = world.getBlockState(p)
                        if (isExpectedMiddleLayerPos(offset)) {
                            // 中层四边中心：允许一个主体块，其余耐火砖
                            if (!state.`is`(CokeKilnBlock::class.instance()) && !state.`is`(RefractoryBricksBlock::class.instance())) {
                                return false
                            }
                        } else {
                            if (!state.`is`(RefractoryBricksBlock::class.instance())) return false
                        }
                    }
                }
            }

            // 中层四边中心必须且仅有 1 个主体块
            var kilnCount = 0
            val sideCenters = listOf(
                BlockPos(hatchPos.x + 1, hatchPos.y - 1, hatchPos.z),
                BlockPos(hatchPos.x - 1, hatchPos.y - 1, hatchPos.z),
                BlockPos(hatchPos.x, hatchPos.y - 1, hatchPos.z + 1),
                BlockPos(hatchPos.x, hatchPos.y - 1, hatchPos.z - 1)
            )
            for (p in sideCenters) {
                if (world.getBlockState(p).`is`(CokeKilnBlock::class.instance())) kilnCount++
            }
            return kilnCount == 1
        }
    }
}

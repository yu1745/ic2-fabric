package ic2_120.content.block

import ic2_120.compat.save

import net.minecraft.world.level.block.state.BlockState

import net.minecraft.world.level.block.state.BlockBehaviour

import net.minecraft.core.registries.BuiltInRegistries

import net.minecraft.core.registries.Registries

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.has

import ic2_120.compat.world

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.FenceBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.PressurePlateBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.SaplingBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.grower.TreeGrower
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.block.state.properties.WoodType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.resources.ResourceKey
import net.minecraft.core.Holder
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.Identifier
import net.minecraft.util.StringRepresentable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.server.level.ServerLevel
import java.util.function.Consumer

// ========== 原木 / 木材 ==========

/** 橡胶原木（树干）。支持 0-2 个侧面随机可提取橡胶，湿面可提取粘性树脂，提取后变为干面。 */
@ModBlock(name = "rubber_wood", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberWood : RotatedPillarBlock(
    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2.0f)
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val log = RubberLogBlock::class.item()
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.BUILDING_BLOCKS, RubberWood::class.item(), 3)
                .pattern("LL").pattern("LL")
                .define('L', log)
                .unlockedBy(getHasName(log), has(log))
                .save(exporter, RubberWood::class.id())
        }
    }
}

/** 橡胶树原木侧面橡胶状态：无槽位、湿（可提取）、干（已提取） */
enum class RubberFaceState(private val id: String) : StringRepresentable {
    NONE("none"), WET("wet"), DRY("dry");
    override fun getSerializedName() = id
}

/** 橡胶树原木。仅自然生成的原木会随机生成可提取槽位，玩家放置的原木不会产出树脂。 */
@ModBlock(name = "rubber_log", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberLogBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2.0f).randomTicks()) : RotatedPillarBlock(settings), EntityBlock {

    init {
        registerDefaultState(
            stateDefinition.any()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.Y)
            .setValue(RUBBER_NORTH, RubberFaceState.NONE)
            .setValue(RUBBER_SOUTH, RubberFaceState.NONE)
            .setValue(RUBBER_EAST, RubberFaceState.NONE)
            .setValue(RUBBER_WEST, RubberFaceState.NONE)
            .setValue(NATURAL, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(RUBBER_NORTH, RUBBER_SOUTH, RUBBER_EAST, RUBBER_WEST, NATURAL)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        RubberLogBlockEntity(pos, state)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onPlace(state, world, pos, oldState, notify)
    }

    fun getRubberState(state: BlockState, face: Direction): RubberFaceState =
        state.getValue(propFor(face))

    fun setFaceDry(state: BlockState, face: Direction): BlockState =
        state.setValue(propFor(face), RubberFaceState.DRY)

    // 对齐 Forge 1.12：破坏含橡胶面的原木有 1/6 概率额外掉落粘性树脂。
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getDrops(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> {
        val drops = super.getDrops(state, builder).toMutableList()
        if (!hasNoRubberFaces(state) && builder.getLevel().random.nextInt(6) == 0) {
            val resinId = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "resin")
            drops.add(ItemStack(BuiltInRegistries.ITEM.getValue(resinId)))
        }
        return drops
    }

    // randomTick：1/7 概率恢复 DRY → WET，对齐 Forge 1.12 BlockRubWood.randomTick 行为。
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (!hasDryFace(state)) return
        if (random.nextInt(7) != 0) return
        if (!hasConnectedRubberLeaves(world, pos)) return

        var newState = state
        for (face in listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            if (getRubberState(state, face) == RubberFaceState.DRY) {
                newState = newState.setValue(propFor(face), RubberFaceState.WET)
            }
        }
        if (newState != state) {
            world.setBlock(pos, newState, 3)
        }
    }

    companion object {
        val RUBBER_NORTH: EnumProperty<RubberFaceState> = EnumProperty.create("rubber_north", RubberFaceState::class.java)
        val RUBBER_SOUTH: EnumProperty<RubberFaceState> = EnumProperty.create("rubber_south", RubberFaceState::class.java)
        val RUBBER_EAST: EnumProperty<RubberFaceState> = EnumProperty.create("rubber_east", RubberFaceState::class.java)
        val RUBBER_WEST: EnumProperty<RubberFaceState> = EnumProperty.create("rubber_west", RubberFaceState::class.java)
        val NATURAL: BooleanProperty = BooleanProperty.create("natural")

        fun propFor(face: Direction): EnumProperty<RubberFaceState> = when (face) {
            Direction.NORTH -> RUBBER_NORTH
            Direction.SOUTH -> RUBBER_SOUTH
            Direction.EAST -> RUBBER_EAST
            Direction.WEST -> RUBBER_WEST
            else -> RUBBER_NORTH
        }

        fun hasNoRubberFaces(state: BlockState): Boolean =
            state.getValue(RUBBER_NORTH) == RubberFaceState.NONE &&
                state.getValue(RUBBER_SOUTH) == RubberFaceState.NONE &&
                state.getValue(RUBBER_EAST) == RubberFaceState.NONE &&
                state.getValue(RUBBER_WEST) == RubberFaceState.NONE

        fun hasDryFace(state: BlockState): Boolean =
            state.getValue(RUBBER_NORTH) == RubberFaceState.DRY ||
                state.getValue(RUBBER_SOUTH) == RubberFaceState.DRY ||
                state.getValue(RUBBER_EAST) == RubberFaceState.DRY ||
                state.getValue(RUBBER_WEST) == RubberFaceState.DRY

        private fun hasConnectedRubberLeaves(world: Level, startPos: BlockPos): Boolean {
            val queue = java.util.ArrayDeque<BlockPos>()
            val visited = HashSet<BlockPos>()

            queue.add(startPos)
            visited.add(startPos)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()

                for (direction in Direction.values()) {
                    val neighborPos = current.relative(direction)
                    val neighborState = world.getBlockState(neighborPos)
                    val neighborBlock = neighborState.block

                    if (neighborBlock is RubberLeavesBlock) {
                        return true
                    }

                    if (neighborBlock is RubberLogBlock && visited.add(neighborPos)) {
                        queue.add(neighborPos.immutable())
                    }
                }
            }

            return false
        }

        fun initializeNaturalState(state: BlockState, random: RandomSource): BlockState {
            val faces = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            val count = pickWetHoleCount(random)
            val indices = (0..3).toMutableList()
            for (i in 0 until count) {
                val j = i + random.nextInt(4 - i)
                indices[i] = indices[j].also { indices[j] = indices[i] }
            }

            var newState = state.setValue(NATURAL, false)
            for (i in 0 until count) {
                newState = newState.setValue(propFor(faces[indices[i]]), RubberFaceState.WET)
            }
            return newState
        }

        private fun pickWetHoleCount(random: RandomSource): Int {
            val config = Ic2Config.current.worldgen.rubberTree.normalized()
            val zeroWeight = config.zeroHoleWeight
            val singleWeight = config.singleHoleWeight
            val doubleWeight = config.doubleHoleWeight
            val totalWeight = zeroWeight + singleWeight + doubleWeight

            if (totalWeight <= 0) {
                // 回退到旧默认值：0孔 11、1孔 2、2孔 1。
                val fallback = random.nextInt(14)
                return when {
                    fallback < 11 -> 0
                    fallback < 13 -> 1
                    else -> 2
                }
            }

            val roll = random.nextInt(totalWeight)
            return when {
                roll < zeroWeight -> 0
                roll < zeroWeight + singleWeight -> 1
                else -> 2
            }
        }
    }
}

@ModBlock(name = "stripped_rubber_log", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class StrippedRubberLogBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).strength(2.0f)) : RotatedPillarBlock(settings)

@ModBlock(name = "stripped_rubber_wood", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class StrippedRubberWoodBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).strength(2.0f)) : RotatedPillarBlock(settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val log = StrippedRubberLogBlock::class.item()
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.BUILDING_BLOCKS, StrippedRubberWoodBlock::class.item(), 3)
                .pattern("LL").pattern("LL")
                .define('L', log)
                .unlockedBy(getHasName(log), has(log))
                .save(exporter, StrippedRubberWoodBlock::class.id())
        }
    }
}

@ModBlock(name = "rubber_planks", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberPlanksBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0f)) : Block(settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val log = RubberLogBlock::class.item()
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.BUILDING_BLOCKS, RubberPlanksBlock::class.item(), 4)
                .requires(ic2_120.compat.tagIngredient(ModTags.RUBBER_LOGS))
                .unlockedBy(getHasName(log), has(log))
                .save(exporter, RubberPlanksBlock::class.id())
        }
    }
}

// ========== 台阶 / 楼梯 ==========

@ModBlock(name = "rubber_slab", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberSlabBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).strength(2.0f)) : SlabBlock(settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.BUILDING_BLOCKS, RubberSlabBlock::class.item(), 6)
                .pattern("PPP").define('P', planks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberSlabBlock::class.id())
        }
    }
}

@ModBlock(name = "rubber_stairs", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberStairsBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).strength(2.0f)) : StairBlock(Blocks.OAK_PLANKS.defaultBlockState(), settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.BUILDING_BLOCKS, RubberStairsBlock::class.item(), 4)
                .pattern("P  ").pattern("PP ").pattern("PPP").define('P', planks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberStairsBlock::class.id())
        }
    }
}

// ========== 栅栏 / 栅栏门 ==========

@ModBlock(name = "rubber_fence", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberFenceBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).strength(2.0f)) : FenceBlock(settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            val sticks = Items.STICK
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, RubberFenceBlock::class.item(), 3)
                .pattern("PSP").pattern("PSP").define('P', planks).define('S', sticks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberFenceBlock::class.id())
        }
    }
}

@ModBlock(name = "rubber_fence_gate", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberFenceGateBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).strength(2.0f)) : FenceGateBlock(WoodType.OAK, settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            val sticks = Items.STICK
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.REDSTONE, RubberFenceGateBlock::class.item(), 1)
                .pattern("SPS").pattern("SPS").define('P', planks).define('S', sticks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberFenceGateBlock::class.id())
        }
    }
}

// ========== 门 / 活板门 ==========

@ModBlock(name = "rubber_door", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberDoorBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).strength(2.0f)) : DoorBlock(BlockSetType.OAK, settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            val sticks = Items.STICK
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.REDSTONE, RubberDoorBlock::class.item(), 3)
                .pattern("PP").pattern("PP").pattern("SS").define('P', planks).define('S', sticks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberDoorBlock::class.id())
        }
    }
}

@ModBlock(name = "rubber_trapdoor", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberTrapdoorBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).strength(2.0f)) : TrapDoorBlock(BlockSetType.OAK, settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.REDSTONE, RubberTrapdoorBlock::class.item(), 2)
                .pattern("PPP").pattern("PPP").define('P', planks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberTrapdoorBlock::class.id())
        }
    }
}

// ========== 按钮 / 压力板 ==========

@ModBlock(name = "rubber_button", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberButtonBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON).strength(2.0f)) : ButtonBlock(BlockSetType.OAK, 30, settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.REDSTONE, RubberButtonBlock::class.item(), 1)
                .requires(planks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberButtonBlock::class.id())
        }
    }
}

@ModBlock(name = "rubber_pressure_plate", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberPressurePlateBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).strength(2.0f)) : PressurePlateBlock(BlockSetType.OAK, settings) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val planks = RubberPlanksBlock::class.item()
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.REDSTONE, RubberPressurePlateBlock::class.item(), 1)
                .pattern("PP").define('P', planks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(exporter, RubberPressurePlateBlock::class.id())
        }
    }
}

// ========== 树叶 / 树苗 ==========

/** 橡胶树生成器，用于树苗生长与骨粉催熟。 */
internal object RubberSaplingGenerator {
    val GROWER: TreeGrower = TreeGrower(
        "rubber_tree",
        java.util.Optional.empty(),
        java.util.Optional.of(
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "rubber_tree"))
        ),
        java.util.Optional.empty()
    )

    /**
     * 标记当前 [TreeGrower.growTree] 调用来自树苗生长路径。
     * [ic2_120.content.worldgen.RubberTreeFeature.isSaplingGrowth] 优先读这个标志，
     * 再用方块检查作为 fallback。
     *
     * 由 [RubberSaplingBlock.advanceTree] 在调用 super 前后 set/remove。
     */
    @JvmStatic
    val SAPLING_GROWTH_FLAG: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
}

@ModBlock(name = "rubber_sapling", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood", renderLayer = "cutout")
class RubberSaplingBlock(
    settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).strength(0.0f)
) : SaplingBlock(RubberSaplingGenerator.GROWER, settings) {
    override fun advanceTree(level: ServerLevel, pos: BlockPos, state: BlockState, random: RandomSource) {
        RubberSaplingGenerator.SAPLING_GROWTH_FLAG.set(true)
        try {
            super.advanceTree(level, pos, state, random)
        } finally {
            RubberSaplingGenerator.SAPLING_GROWTH_FLAG.remove()
        }
    }
}

@ModBlock(name = "rubber_leaves", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberLeavesBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).strength(0.2f)) : net.minecraft.world.level.block.TintedParticleLeavesBlock(0.01f, settings) {
    override fun codec(): com.mojang.serialization.MapCodec<out net.minecraft.world.level.block.TintedParticleLeavesBlock> =
        net.minecraft.world.level.block.Block.simpleCodec(::RubberLeavesBlock)

    init {
        // 明确指定默认值，避免新增布尔状态在不同注册阶段被误认为已绑定树苗。
        registerDefaultState(defaultBlockState().setValue(RUBBER_SAPLING_DROP, false))
    }

    companion object {
        val RUBBER_SAPLING_DROP: BooleanProperty = BooleanProperty.create("rubber_sapling_drop")
        val RUBBER_TREE_GENERATED: BooleanProperty = BooleanProperty.create("rubber_tree_generated")
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(RUBBER_SAPLING_DROP, RUBBER_TREE_GENERATED)
    }

    /**
     * 橡胶叶的距离传播不能直接使用 vanilla LeavesBlock 的实现：
     * vanilla 会把其他树种的原木/树叶也当成支撑，导致橡胶叶挂在橡树叶等叶子上时不腐烂。
     * 橡胶叶只允许连接到橡胶原木或橡胶叶链。
     */
    override fun updateShape(
        state: BlockState,
        world: net.minecraft.world.level.LevelReader,
        ticks: net.minecraft.world.level.ScheduledTickAccess,
        pos: BlockPos,
        direction: Direction,
        neighborPos: BlockPos,
        neighborState: BlockState,
        random: RandomSource
    ): BlockState {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.WATER.getTickDelay(world))
        }
        ticks.scheduleTick(pos, this, 1)
        return state
    }

    override fun tick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        val distance = updateDistanceFromRubberLogs(state, world, pos)
        if (distance != state.getValue(DISTANCE)) {
            world.setBlock(pos, state.setValue(DISTANCE, distance), Block.UPDATE_ALL)
        }
    }

    override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        // 重新计算一次，修正旧版本曾沿着其他树叶链传播过来的错误距离值。
        val distance = updateDistanceFromRubberLogs(state, world, pos)
        val updatedState = state.setValue(DISTANCE, distance)
        if (!updatedState.getValue(PERSISTENT) && distance == LeavesBlock.DECAY_DISTANCE) {
            Block.dropResources(updatedState, world, pos)
            world.removeBlock(pos, false)
        } else if (updatedState != state) {
            world.setBlock(pos, updatedState, Block.UPDATE_ALL)
        }
    }

    private fun updateDistanceFromRubberLogs(state: BlockState, world: LevelAccessor, pos: BlockPos): Int {
        var distance = LeavesBlock.DECAY_DISTANCE
        val mutable = BlockPos.MutableBlockPos()
        for (direction in Direction.values()) {
            mutable.setWithOffset(pos, direction)
            distance = minOf(distance, getRubberDistance(world.getBlockState(mutable)) + 1)
            if (distance == 1) break
        }
        return distance
    }

    private fun getRubberDistance(state: BlockState): Int = when {
        state.block is RubberLogBlock -> 0
        state.block is RubberLeavesBlock && state.hasProperty(DISTANCE) -> state.getValue(DISTANCE)
        else -> LeavesBlock.DECAY_DISTANCE
    }
}

/**
 * 树苗绑定在具体叶子上：生成树时把保底树苗标记写成方块状态 `rubber_sapling_drop`，
 * 由橡胶叶战利品表（[ModBlockLootTableProvider] 生成）在破坏/凋落时读取兑现。
 * 放在方块状态里（而非代码钩子）是为了让动力锯这类"只走战利品表"的破坏路径也能触发掉落。
 */
internal object RubberTreeSaplingDrop {
    fun bindLeaves(world: LevelAccessor, leafPositions: List<BlockPos>, random: RandomSource) {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        if (!config.saplingGuaranteeEnabled || leafPositions.isEmpty()) return

        val expected = config.saplingDropExpected.coerceAtLeast(1f)
        val extra = expected - 1f
        var count = 1 + extra.toInt()
        if (random.nextFloat() < extra - extra.toInt()) count++

        val candidates = leafPositions.map { it.immutable() }.toMutableList()
        leafPositions.forEach { leafPos ->
            val state = world.getBlockState(leafPos)
            if (state.block is RubberLeavesBlock) {
                world.setBlock(leafPos, state.setValue(RubberLeavesBlock.RUBBER_TREE_GENERATED, true), Block.UPDATE_ALL)
            }
        }
        repeat(count.coerceAtMost(candidates.size)) {
            val leafPos = candidates.removeAt(random.nextInt(candidates.size))
            val state = world.getBlockState(leafPos)
            if (state.block is RubberLeavesBlock) {
                world.setBlock(leafPos, state.setValue(RubberLeavesBlock.RUBBER_SAPLING_DROP, true), Block.UPDATE_ALL)
            }
        }
    }
}

// ========== 告示牌 ==========

//todo 材质有点问题，先禁用
// @ModBlock(name = "rubber_sign", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
// class RubberSignBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN).strength(1.0f) .noOcclusion()) : net.minecraft.world.level.block.StandingSignBlock(settings, net.minecraft.block.WoodType.OAK)

// @ModBlock(name = "rubber_wall_sign", registerItem = false, tab = CreativeTab.MINECRAFT_DECORATIONS, group = "wood")
// class RubberWallSignBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN).strength(1.0f) .noOcclusion().dropsLike(Blocks.OAK_SIGN)) : net.minecraft.world.level.block.WallSignBlock(settings, net.minecraft.block.WoodType.OAK)

package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.BlockTags
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.gen.chunk.ChunkGenerator
import net.minecraft.world.gen.feature.ConfiguredFeature
import net.minecraft.server.world.ServerWorld
import java.util.ArrayDeque

// ========== 原木 / 木材 ==========

/** 橡胶原木（树干）。支持 0-2 个侧面随机可提取橡胶，湿面可提取粘性树脂，提取后变为干面。 */
@ModBlock(name = "rubber_wood", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberWood : PillarBlock(
    AbstractBlock.Settings.copy(Blocks.OAK_LOG).strength(2.0f)
)

/** 橡胶树原木侧面橡胶状态：无槽位、湿（可提取）、干（已提取） */
enum class RubberFaceState(private val id: String) : StringIdentifiable {
    NONE("none"), WET("wet"), DRY("dry");
    override fun asString() = id
}

/** 橡胶树原木。仅自然生成的原木会随机生成可提取槽位，玩家放置的原木不会产出树脂。 */
@ModBlock(name = "rubber_log", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberLogBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_LOG).strength(2.0f).ticksRandomly()) : PillarBlock(settings), BlockEntityProvider {

    init {
        defaultState = stateManager.defaultState
            .with(Properties.AXIS, Direction.Axis.Y)
            .with(RUBBER_NORTH, RubberFaceState.NONE)
            .with(RUBBER_SOUTH, RubberFaceState.NONE)
            .with(RUBBER_EAST, RubberFaceState.NONE)
            .with(RUBBER_WEST, RubberFaceState.NONE)
            .with(NATURAL, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(RUBBER_NORTH, RUBBER_SOUTH, RUBBER_EAST, RUBBER_WEST, NATURAL)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        RubberLogBlockEntity(pos, state)

    // AbstractBlock.onBlockAdded 在 1.20.1 标记为 @Deprecated（Mojang 设计：override 是预期用法，详见其 javadoc）。
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
    }

    fun getRubberState(state: BlockState, face: Direction): RubberFaceState =
        state.get(propFor(face))

    fun setFaceDry(state: BlockState, face: Direction): BlockState =
        state.with(propFor(face), RubberFaceState.DRY)

    // 对齐 Forge 1.12：破坏含橡胶面的原木有 1/6 概率额外掉落粘性树脂。
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getDroppedStacks(state: BlockState, builder: LootContextParameterSet.Builder): MutableList<ItemStack> {
        val drops = super.getDroppedStacks(state, builder)
        if (!hasNoRubberFaces(state) && builder.world.random.nextInt(6) == 0) {
            val resinId = Identifier(Ic2_120.MOD_ID, "resin")
            drops.add(ItemStack(Registries.ITEM.get(resinId)))
        }
        return drops
    }

    // randomTick：1/7 概率恢复 DRY → WET，对齐 Forge 1.12 BlockRubWood.randomTick 行为。
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun randomTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        if (!hasDryFace(state)) return
        if (random.nextInt(7) != 0) return
        if (!hasConnectedRubberLeaves(world, pos)) return

        var newState = state
        for (face in listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            if (getRubberState(state, face) == RubberFaceState.DRY) {
                newState = newState.with(propFor(face), RubberFaceState.WET)
            }
        }
        if (newState != state) {
            world.setBlockState(pos, newState)
        }
    }

    companion object {
        val RUBBER_NORTH: EnumProperty<RubberFaceState> = EnumProperty.of("rubber_north", RubberFaceState::class.java)
        val RUBBER_SOUTH: EnumProperty<RubberFaceState> = EnumProperty.of("rubber_south", RubberFaceState::class.java)
        val RUBBER_EAST: EnumProperty<RubberFaceState> = EnumProperty.of("rubber_east", RubberFaceState::class.java)
        val RUBBER_WEST: EnumProperty<RubberFaceState> = EnumProperty.of("rubber_west", RubberFaceState::class.java)
        val NATURAL: BooleanProperty = BooleanProperty.of("natural")

        fun propFor(face: Direction): EnumProperty<RubberFaceState> = when (face) {
            Direction.NORTH -> RUBBER_NORTH
            Direction.SOUTH -> RUBBER_SOUTH
            Direction.EAST -> RUBBER_EAST
            Direction.WEST -> RUBBER_WEST
            else -> RUBBER_NORTH
        }

        fun hasNoRubberFaces(state: BlockState): Boolean =
            state.get(RUBBER_NORTH) == RubberFaceState.NONE &&
                state.get(RUBBER_SOUTH) == RubberFaceState.NONE &&
                state.get(RUBBER_EAST) == RubberFaceState.NONE &&
                state.get(RUBBER_WEST) == RubberFaceState.NONE

        fun hasDryFace(state: BlockState): Boolean =
            state.get(RUBBER_NORTH) == RubberFaceState.DRY ||
                state.get(RUBBER_SOUTH) == RubberFaceState.DRY ||
                state.get(RUBBER_EAST) == RubberFaceState.DRY ||
                state.get(RUBBER_WEST) == RubberFaceState.DRY

        private fun hasConnectedRubberLeaves(world: World, startPos: BlockPos): Boolean {
            val queue = java.util.ArrayDeque<BlockPos>()
            val visited = HashSet<BlockPos>()

            queue.add(startPos)
            visited.add(startPos)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()

                for (direction in Direction.values()) {
                    val neighborPos = current.offset(direction)
                    val neighborState = world.getBlockState(neighborPos)
                    val neighborBlock = neighborState.block

                    if (neighborBlock is RubberLeavesBlock) {
                        return true
                    }

                    if (neighborBlock is RubberLogBlock && visited.add(neighborPos)) {
                        queue.add(neighborPos.toImmutable())
                    }
                }
            }

            return false
        }

        fun initializeNaturalState(state: BlockState, random: Random): BlockState {
            val faces = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            val count = pickWetHoleCount(random)
            val indices = (0..3).toMutableList()
            for (i in 0 until count) {
                val j = i + random.nextInt(4 - i)
                indices[i] = indices[j].also { indices[j] = indices[i] }
            }

            var newState = state.with(NATURAL, false)
            for (i in 0 until count) {
                newState = newState.with(propFor(faces[indices[i]]), RubberFaceState.WET)
            }
            return newState
        }

        private fun pickWetHoleCount(random: Random): Int {
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
class StrippedRubberLogBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_LOG).strength(2.0f)) : PillarBlock(settings)

@ModBlock(name = "stripped_rubber_wood", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class StrippedRubberWoodBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.0f)) : PillarBlock(settings)

@ModBlock(name = "rubber_planks", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberPlanksBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0f)) : Block(settings)

// ========== 台阶 / 楼梯 ==========

@ModBlock(name = "rubber_slab", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberSlabBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SLAB).strength(2.0f)) : SlabBlock(settings)

@ModBlock(name = "rubber_stairs", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberStairsBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_STAIRS).strength(2.0f)) : StairsBlock(Blocks.OAK_PLANKS.defaultState, settings)

// ========== 栅栏 / 栅栏门 ==========

@ModBlock(name = "rubber_fence", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberFenceBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_FENCE).strength(2.0f)) : FenceBlock(settings)

@ModBlock(name = "rubber_fence_gate", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberFenceGateBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).strength(2.0f)) : FenceGateBlock(settings, WoodType.OAK)

// ========== 门 / 活板门 ==========

@ModBlock(name = "rubber_door", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberDoorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_DOOR).strength(2.0f)) : DoorBlock(settings, BlockSetType.OAK)

@ModBlock(name = "rubber_trapdoor", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberTrapdoorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).strength(2.0f)) : TrapdoorBlock(settings, BlockSetType.OAK)

// ========== 按钮 / 压力板 ==========

@ModBlock(name = "rubber_button", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberButtonBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_BUTTON).strength(2.0f)) : ButtonBlock(settings, BlockSetType.OAK, 30, true)

@ModBlock(name = "rubber_pressure_plate", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberPressurePlateBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE).strength(2.0f)) : PressurePlateBlock(PressurePlateBlock.ActivationRule.EVERYTHING, settings, BlockSetType.OAK)

// ========== 树叶 / 树苗 ==========

/** 橡胶树生成器，用于树苗生长与骨粉催熟。 */
internal class RubberSaplingGenerator : net.minecraft.block.sapling.SaplingGenerator() {
    override fun getTreeFeature(random: net.minecraft.util.math.random.Random, bees: Boolean): RegistryKey<ConfiguredFeature<*, *>>? =
        RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier(Ic2_120.MOD_ID, "rubber_tree"))

    /**
     * 覆写父类 [SaplingGenerator.generate]，目的有二：
     *
     * 1. **必须**在调用 feature 之前把树苗替换为流体状态（vanilla 行为），否则
     *    [net.minecraft.world.gen.feature.TreeFeature.getTopPosition] 会在 i=0 处看到
     *    sapling 方块并直接 `i - 2`，导致树干放不下，树永远不会长出来。
     * 2. 由于 (1) 中树苗被替换成 AIR，feature 内 `getBlockState(origin).block is RubberSaplingBlock`
     *    永远为 false，无法再区分"树苗生长"和"自然世界生成"两种来源。
     *    这里用 [SAPLING_GROWTH_FLAG] ThreadLocal 标记当前调用来自树苗路径，
     *    [RubberTreeFeature.isSaplingGrowth] 看到这个标记后会走树苗分支（保留周围方块），
     *    而不是世界生成分支（清除/替换周围原木）。
     */
    override fun generate(
        world: ServerWorld,
        chunkGenerator: ChunkGenerator,
        pos: BlockPos,
        state: BlockState,
        random: Random
    ): Boolean {
        val registryKey = getTreeFeature(random, areFlowersNearby(world, pos)) ?: return false
        val registryEntry: RegistryEntry<ConfiguredFeature<*, *>> = world.registryManager
            .get(RegistryKeys.CONFIGURED_FEATURE)
            .getEntry(registryKey)
            .orElse(null) ?: return false
        val configuredFeature = registryEntry.value()

        // 关键差异：与 vanilla 一样先把树苗替换为流体状态（无水 = AIR），
        // 然后用 ThreadLocal 告诉 feature 这次是树苗生长而非世界生成。
        val fluidState = world.getFluidState(pos).getBlockState()
        world.setBlockState(pos, fluidState, Block.NO_REDRAW)
        SAPLING_GROWTH_FLAG.set(true)
        return try {
            if (configuredFeature.generate(world, chunkGenerator, random, pos)) {
                if (world.getBlockState(pos) == fluidState) {
                    world.updateListeners(pos, state, fluidState, Block.NOTIFY_LISTENERS)
                }
                true
            } else {
                world.setBlockState(pos, state, Block.NO_REDRAW)
                false
            }
        } finally {
            SAPLING_GROWTH_FLAG.remove()
        }
    }

    private fun areFlowersNearby(world: WorldAccess, pos: BlockPos): Boolean {
        for (flowerPos in BlockPos.Mutable.iterate(pos.down().north(2).west(2), pos.up().south(2).east(2))) {
            if (world.getBlockState(flowerPos).isIn(BlockTags.FLOWERS)) {
                return true
            }
        }
        return false
    }

    companion object {
        /**
         * 标记下一次 [SaplingGenerator.generate] 的调用来自树苗生长路径。
         * [RubberTreeFeature.isSaplingGrowth] 优先读这个标志，再用方块检查作为 fallback。
         */
        @JvmStatic
        val SAPLING_GROWTH_FLAG: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    }
}

@ModBlock(name = "rubber_leaves", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood", generateBlockLootTable = false)
class RubberLeavesBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).strength(0.2f)) : LeavesBlock(settings) {
    init {
        // 明确指定默认值，避免新增布尔状态在不同注册阶段被误认为已绑定树苗。
        defaultState = defaultState.with(RUBBER_SAPLING_DROP, false)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStacksDropped(state: BlockState, world: ServerWorld, pos: BlockPos, tool: ItemStack, dropExperience: Boolean) {
        super.onStacksDropped(state, world, pos, tool, dropExperience)
        val normalTool = !tool.isOf(Items.SHEARS) && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, tool) == 0
        if (state.get(RUBBER_SAPLING_DROP) && normalTool) {
            Block.dropStack(world, pos, ItemStack(RubberSaplingBlock::class.item()))
        } else if (normalTool && Ic2Config.current.worldgen.rubberTree.normalized().saplingGuaranteeEnabled &&
            !state.get(RUBBER_TREE_GENERATED)
        ) {
            RubberTreeSaplingDrop.migrateLegacyTree(world, pos, world.random)
        } else if (!Ic2Config.current.worldgen.rubberTree.normalized().saplingGuaranteeEnabled &&
            normalTool &&
            world.random.nextFloat() < 0.0225f
        ) {
            Block.dropStack(world, pos, ItemStack(RubberSaplingBlock::class.item()))
        }
    }

    companion object {
        val RUBBER_SAPLING_DROP: BooleanProperty = BooleanProperty.of("rubber_sapling_drop")
        val RUBBER_TREE_GENERATED: BooleanProperty = BooleanProperty.of("rubber_tree_generated")
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(RUBBER_SAPLING_DROP, RUBBER_TREE_GENERATED)
    }

    /**
     * 橡胶叶的距离传播不能直接使用 vanilla LeavesBlock 的实现：
     * vanilla 会把其他树种的原木/树叶也当成支撑，导致橡胶叶挂在橡树叶等叶子上时不腐烂。
     * 橡胶叶只允许连接到橡胶原木或橡胶叶链。
     */
    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: WorldAccess,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, net.minecraft.fluid.Fluids.WATER, net.minecraft.fluid.Fluids.WATER.getTickRate(world))
        }
        world.scheduleBlockTick(pos, this, 1)
        return state
    }

    override fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        val distance = updateDistanceFromRubberLogs(state, world, pos)
        if (distance != state.get(DISTANCE)) {
            world.setBlockState(pos, state.with(DISTANCE, distance), Block.NOTIFY_ALL)
        }
    }

    override fun randomTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        // 重新计算一次，修正旧版本曾沿着其他树叶链传播过来的错误距离值。
        val distance = updateDistanceFromRubberLogs(state, world, pos)
        val updatedState = state.with(DISTANCE, distance)
        if (!updatedState.get(PERSISTENT) && distance == MAX_DISTANCE) {
            dropStacks(updatedState, world, pos)
            world.removeBlock(pos, false)
        } else if (updatedState != state) {
            world.setBlockState(pos, updatedState, Block.NOTIFY_ALL)
        }
    }

    private fun updateDistanceFromRubberLogs(state: BlockState, world: WorldAccess, pos: BlockPos): Int {
        var distance = MAX_DISTANCE
        val mutable = BlockPos.Mutable()
        for (direction in Direction.values()) {
            mutable.set(pos, direction)
            distance = minOf(distance, getRubberDistance(world.getBlockState(mutable)) + 1)
            if (distance == 1) break
        }
        return distance
    }

    private fun getRubberDistance(state: BlockState): Int = when {
        state.block is RubberLogBlock -> 0
        state.block is RubberLeavesBlock && state.contains(DISTANCE) -> state.get(DISTANCE)
        else -> MAX_DISTANCE
    }
}

/**
 * 树苗绑定在具体叶子上，由叶子消失时触发掉落；不在砍原木时直接掉落。
 */
internal object RubberTreeSaplingDrop {
    fun bindLeaves(world: WorldAccess, leafPositions: List<BlockPos>, random: Random) {
        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        if (!config.saplingGuaranteeEnabled || leafPositions.isEmpty()) return

        val expected = config.saplingDropExpected.coerceAtLeast(1f)
        val extra = expected - 1f
        var count = 1 + extra.toInt()
        if (random.nextFloat() < extra - extra.toInt()) count++

        val candidates = leafPositions.map { it.toImmutable() }.toMutableList()
        leafPositions.forEach { leafPos ->
            val state = world.getBlockState(leafPos)
            if (state.block is RubberLeavesBlock) {
                world.setBlockState(leafPos, state.with(RubberLeavesBlock.RUBBER_TREE_GENERATED, true), Block.NOTIFY_ALL)
            }
        }
        repeat(count.coerceAtMost(candidates.size)) {
            val leafPos = candidates.removeAt(random.nextInt(candidates.size))
            val state = world.getBlockState(leafPos)
            if (state.block is RubberLeavesBlock) {
                world.setBlockState(leafPos, state.with(RubberLeavesBlock.RUBBER_SAPLING_DROP, true), Block.NOTIFY_ALL)
            }
        }
    }

    fun migrateLegacyTree(world: ServerWorld, start: BlockPos, random: Random) {
        val leaves = connectedLeaves(world, start)
        if (leaves.isEmpty()) return

        // 当前正在消失的旧叶子承担保底 1 个。
        Block.dropStack(world, start, ItemStack(RubberSaplingBlock::class.item()))

        val config = Ic2Config.current.worldgen.rubberTree.normalized()
        val extra = config.saplingDropExpected.coerceAtLeast(1f) - 1f
        var extraCount = extra.toInt()
        if (random.nextFloat() < extra - extra.toInt()) extraCount++

        val candidates = leaves.filter { it != start }.toMutableList()
        leaves.forEach { leafPos ->
            val state = world.getBlockState(leafPos)
            if (state.block is RubberLeavesBlock) {
                world.setBlockState(leafPos, state.with(RubberLeavesBlock.RUBBER_TREE_GENERATED, true), Block.NOTIFY_ALL)
            }
        }
        repeat(extraCount.coerceAtMost(candidates.size)) {
            val leafPos = candidates.removeAt(random.nextInt(candidates.size))
            val state = world.getBlockState(leafPos)
            if (state.block is RubberLeavesBlock) {
                world.setBlockState(leafPos, state.with(RubberLeavesBlock.RUBBER_SAPLING_DROP, true), Block.NOTIFY_ALL)
            }
        }
    }

    private fun connectedLeaves(world: ServerWorld, start: BlockPos): List<BlockPos> {
        val queue = ArrayDeque<BlockPos>()
        val visited = HashSet<BlockPos>()
        val leaves = mutableListOf<BlockPos>()
        queue.add(start.toImmutable())
        visited.add(start.toImmutable())
        while (queue.isNotEmpty() && visited.size <= 512) {
            val pos = queue.removeFirst()
            val state = world.getBlockState(pos)
            if (state.block is RubberLeavesBlock) leaves += pos.toImmutable()
            if (state.block !is RubberLeavesBlock && state.block !is RubberLogBlock) continue
            for (direction in Direction.values()) {
                val next = pos.offset(direction).toImmutable()
                if (!visited.add(next)) continue
                val block = world.getBlockState(next).block
                if (block is RubberLeavesBlock || block is RubberLogBlock) queue.add(next)
            }
        }
        return leaves
    }
}

/** 橡胶树苗，支持骨粉催熟与自然生长。 */
@ModBlock(name = "rubber_sapling", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood", renderLayer = "cutout")
class RubberSaplingBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SAPLING).strength(0.0f)
) : SaplingBlock(RubberSaplingGenerator(), settings)

// ========== 告示牌 ==========

//todo 材质有点问题，先禁用
// @ModBlock(name = "rubber_sign", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
// class RubberSignBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SIGN).strength(1.0f).nonOpaque()) : net.minecraft.block.SignBlock(settings, net.minecraft.block.WoodType.OAK)

// @ModBlock(name = "rubber_wall_sign", registerItem = false, tab = CreativeTab.MINECRAFT_DECORATIONS, group = "wood")
// class RubberWallSignBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_WALL_SIGN).strength(1.0f).nonOpaque().dropsLike(Blocks.OAK_SIGN)) : net.minecraft.block.WallSignBlock(settings, net.minecraft.block.WoodType.OAK)

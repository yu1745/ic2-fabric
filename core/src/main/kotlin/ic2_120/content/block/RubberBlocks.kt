package ic2_120.content.block

import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import java.util.Optional
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.ItemPlacementContext
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
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
import net.minecraft.world.gen.feature.ConfiguredFeature

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
class RubberLogBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_LOG).strength(2.0f)) : BlockWithEntity(settings) {
    override fun getCodec(): MapCodec<out BlockWithEntity> = RUBBER_LOG_CODEC

    init {
        defaultState = stateManager.defaultState
            .with(Properties.AXIS, Direction.Axis.Y)
            .with(RUBBER_NORTH, RubberFaceState.NONE)
            .with(RUBBER_SOUTH, RubberFaceState.NONE)
            .with(RUBBER_EAST, RubberFaceState.NONE)
            .with(RUBBER_WEST, RubberFaceState.NONE)
            .with(NATURAL, false)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        RubberLogBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, RubberLogBlockEntity::class.type()){ w, p, s, be -> RubberLogBlockEntity.tick(w, p, s, be as RubberLogBlockEntity) }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.AXIS, RUBBER_NORTH, RUBBER_SOUTH, RUBBER_EAST, RUBBER_WEST, NATURAL)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(Properties.AXIS, ctx.side.axis)

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient && state.get(NATURAL) && hasNoRubberFaces(state)) {
            world.setBlockState(pos, initializeNaturalState(state, world.random))
        }
    }

    fun getRubberState(state: BlockState, face: Direction): RubberFaceState =
        state.get(propFor(face))

    fun setFaceDry(state: BlockState, face: Direction): BlockState =
        state.with(propFor(face), RubberFaceState.DRY)

    companion object {
        val RUBBER_LOG_CODEC: MapCodec<RubberLogBlock> = Block.createCodec(::RubberLogBlock)
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
class RubberFenceGateBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).strength(2.0f)) : FenceGateBlock(WoodType.OAK, settings)

// ========== 门 / 活板门 ==========

@ModBlock(name = "rubber_door", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberDoorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_DOOR).strength(2.0f)) : DoorBlock(BlockSetType.OAK, settings)

@ModBlock(name = "rubber_trapdoor", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberTrapdoorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).strength(2.0f)) : TrapdoorBlock(BlockSetType.OAK, settings)

// ========== 按钮 / 压力板 ==========

@ModBlock(name = "rubber_button", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberButtonBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_BUTTON).strength(2.0f)) : ButtonBlock(BlockSetType.OAK, 30, settings)

@ModBlock(name = "rubber_pressure_plate", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberPressurePlateBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE).strength(2.0f)) : PressurePlateBlock(BlockSetType.OAK, settings)

// ========== 树叶 / 树苗 ==========

/** 橡胶树生成器，用于树苗生长与骨粉催熟。 */
private val RUBBER_TREE_KEY: RegistryKey<ConfiguredFeature<*, *>> = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(Ic2_120.MOD_ID, "rubber_tree"))

private val RUBBER_SAPLING_GENERATOR = SaplingGenerator(
    "rubber_tree",
    Optional.empty<RegistryKey<ConfiguredFeature<*, *>>>(),
    Optional.of(RUBBER_TREE_KEY),
    Optional.empty<RegistryKey<ConfiguredFeature<*, *>>>()
)

@ModBlock(name = "rubber_leaves", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood", generateBlockLootTable = false)
class RubberLeavesBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).strength(0.2f)) : LeavesBlock(settings)

/** 橡胶树苗，支持骨粉催熟与自然生长。 */
@ModBlock(name = "rubber_sapling", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood", renderLayer = "cutout")
class RubberSaplingBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SAPLING).strength(0.0f)
) : SaplingBlock(RUBBER_SAPLING_GENERATOR, settings)

// ========== 告示牌 ==========

//todo 材质有点问题，先禁用
// @ModBlock(name = "rubber_sign", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
// class RubberSignBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SIGN).strength(1.0f).nonOpaque()) : net.minecraft.block.SignBlock(settings, net.minecraft.block.WoodType.OAK)

// @ModBlock(name = "rubber_wall_sign", registerItem = false, tab = CreativeTab.MINECRAFT_DECORATIONS, group = "wood")
// class RubberWallSignBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_WALL_SIGN).strength(1.0f).nonOpaque().dropsLike(Blocks.OAK_SIGN)) : net.minecraft.block.WallSignBlock(settings, net.minecraft.block.WoodType.OAK)

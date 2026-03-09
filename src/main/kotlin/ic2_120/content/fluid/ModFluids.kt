package ic2_120.content.fluid

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FluidBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.fluid.FlowableFluid
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.item.BucketItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldView

/**
 * IC2 模组流体注册。
 * 基于 Fabric Wiki 流体教程实现，参考 /websites/fabricmc_net_develop 与 Fabric 官方文档。
 *
 * 流体纹理路径约定：ic2:block/fluid/{name}_still, ic2:block/fluid/{name}_flow
 * 无 flow 纹理时使用 still 纹理作为 flowing。
 */
object ModFluids {

    private const val TEXTURE_NS = "ic2"

    // Coolant - 冷却液（核反应堆）
    val COOLANT_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "coolant")) as FlowableFluid }
    val COOLANT_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_coolant")) as FlowableFluid }
    lateinit var COOLANT_BLOCK: Block
    lateinit var COOLANT_BUCKET: Item

    // Hot Coolant - 热冷却液
    val HOT_COOLANT_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "hot_coolant")) as FlowableFluid }
    val HOT_COOLANT_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_hot_coolant")) as FlowableFluid }
    lateinit var HOT_COOLANT_BLOCK: Block
    lateinit var HOT_COOLANT_BUCKET: Item

    // UU Matter - UU物质
    val UU_MATTER_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "uu_matter")) as FlowableFluid }
    val UU_MATTER_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_uu_matter")) as FlowableFluid }
    lateinit var UU_MATTER_BLOCK: Block
    lateinit var UU_MATTER_BUCKET: Item

    // Weed-EX - 除草剂
    val WEED_EX_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "weed_ex")) as FlowableFluid }
    val WEED_EX_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_weed_ex")) as FlowableFluid }
    lateinit var WEED_EX_BLOCK: Block
    lateinit var WEED_EX_BUCKET: Item

    // Pahoehoe Lava - 熔岩岩浆
    val PAHOEHOE_LAVA_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "pahoehoe_lava")) as FlowableFluid }
    val PAHOEHOE_LAVA_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_pahoehoe_lava")) as FlowableFluid }
    lateinit var PAHOEHOE_LAVA_BLOCK: Block
    lateinit var PAHOEHOE_LAVA_BUCKET: Item

    fun register() {
        registerFluid("coolant", "coolant", "coolant")
        registerFluid("hot_coolant", "hot_coolant", "hot_coolant")
        registerFluid("uu_matter", "uu_matter", "uu_matter")
        registerFluid("weed_ex", "weed_ex", "weed_ex")  // 仅 still，flow 用 still
        registerFluid("pahoehoe_lava", "pahoehoe_lava", "pahoehoe_lava")  // 仅 still，flow 用 still
    }

    private fun registerFluid(name: String, stillTex: String, flowTex: String) {
        val modId = Ic2_120.MOD_ID

        // 1. 注册 Still 和 Flowing 流体
        val still = Registry.register(
            Registries.FLUID,
            Identifier(modId, name),
            Ic2Fluid.Still(name, stillTex, flowTex)
        )
        val flowing = Registry.register(
            Registries.FLUID,
            Identifier(modId, "flowing_$name"),
            Ic2Fluid.Flowing(name, stillTex, flowTex)
        )

        // 2. 注册 FluidBlock
        val block = Registry.register(
            Registries.BLOCK,
            Identifier(modId, name),
            FluidBlock(still as FlowableFluid, AbstractBlock.Settings.copy(Blocks.WATER).noCollision().dropsNothing())
        )

        // 3. 注册 Bucket
        val bucket = Registry.register(
            Registries.ITEM,
            Identifier(modId, "${name}_bucket"),
            BucketItem(still, FabricItemSettings().recipeRemainder(Items.BUCKET).maxCount(1))
        )

        // 4. 设置流体类的 block 和 bucket 引用（通过反射或延迟初始化）
        when (name) {
            "coolant" -> {
                COOLANT_BLOCK = block
                COOLANT_BUCKET = bucket
            }
            "hot_coolant" -> {
                HOT_COOLANT_BLOCK = block
                HOT_COOLANT_BUCKET = bucket
            }
            "uu_matter" -> {
                UU_MATTER_BLOCK = block
                UU_MATTER_BUCKET = bucket
            }
            "weed_ex" -> {
                WEED_EX_BLOCK = block
                WEED_EX_BUCKET = bucket
            }
            "pahoehoe_lava" -> {
                PAHOEHOE_LAVA_BLOCK = block
                PAHOEHOE_LAVA_BUCKET = bucket
            }
        }

        // 5. 添加到创造模式物品栏
        val ic2MaterialsKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(modId, CreativeTab.IC2_MATERIALS.id))
        ItemGroupEvents.modifyEntriesEvent(ic2MaterialsKey).register { entries ->
            entries.add(bucket)
        }
    }

    /**
     * IC2 抽象流体基类。
     * 行为类似水：非无限、流动速度 4、每格衰减 1、更新间隔 5 tick。
     */
    abstract class Ic2Fluid(
        protected val name: String,
        private val stillTex: String,
        private val flowTex: String
    ) : FlowableFluid() {

        override fun getStill(): Fluid = getStillFluid()
        override fun getFlowing(): Fluid = getFlowingFluid()
        override fun getBucketItem(): Item = getBucket()
        override fun toBlockState(state: FluidState): BlockState =
            getBlock().defaultState.with(FluidBlock.LEVEL, getBlockStateLevel(state))

        protected abstract fun getStillFluid(): Fluid
        protected abstract fun getFlowingFluid(): Fluid
        protected abstract fun getBlock(): Block
        protected abstract fun getBucket(): Item

        override fun matchesType(fluid: Fluid): Boolean = fluid == getStillFluid() || fluid == getFlowingFluid()
        override fun isInfinite(world: World): Boolean = false
        override fun beforeBreakingBlock(world: net.minecraft.world.WorldAccess, pos: net.minecraft.util.math.BlockPos, state: BlockState) {
            val entity = if (state.hasBlockEntity()) world.getBlockEntity(pos) else null
            Block.dropStacks(state, world, pos, entity)
        }
        override fun canBeReplacedWith(
            state: FluidState,
            world: BlockView,
            pos: BlockPos,
            fluid: Fluid,
            direction: Direction
        ): Boolean = false
        override fun getFlowSpeed(world: WorldView): Int = 4
        override fun getLevelDecreasePerBlock(world: WorldView): Int = 1
        override fun getTickRate(world: WorldView): Int = 5
        override fun getBlastResistance(): Float = 100f

        class Still(name: String, stillTex: String, flowTex: String) : Ic2Fluid(name, stillTex, flowTex) {
            override fun getLevel(state: FluidState): Int = 8
            override fun isStill(state: FluidState): Boolean = true
            override fun getStillFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_STILL
                "hot_coolant" -> HOT_COOLANT_STILL
                "uu_matter" -> UU_MATTER_STILL
                "weed_ex" -> WEED_EX_STILL
                "pahoehoe_lava" -> PAHOEHOE_LAVA_STILL
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getFlowingFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_FLOWING
                "hot_coolant" -> HOT_COOLANT_FLOWING
                "uu_matter" -> UU_MATTER_FLOWING
                "weed_ex" -> WEED_EX_FLOWING
                "pahoehoe_lava" -> PAHOEHOE_LAVA_FLOWING
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBlock(): Block = when (name) {
                "coolant" -> COOLANT_BLOCK
                "hot_coolant" -> HOT_COOLANT_BLOCK
                "uu_matter" -> UU_MATTER_BLOCK
                "weed_ex" -> WEED_EX_BLOCK
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BLOCK
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBucket(): Item = when (name) {
                "coolant" -> COOLANT_BUCKET
                "hot_coolant" -> HOT_COOLANT_BUCKET
                "uu_matter" -> UU_MATTER_BUCKET
                "weed_ex" -> WEED_EX_BUCKET
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BUCKET
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
        }

        class Flowing(name: String, stillTex: String, flowTex: String) : Ic2Fluid(name, stillTex, flowTex) {
            override fun appendProperties(builder: StateManager.Builder<Fluid, FluidState>) {
                super.appendProperties(builder)
                builder.add(LEVEL)
            }
            override fun getLevel(state: FluidState): Int = state.get(LEVEL)
            override fun isStill(state: FluidState): Boolean = false
            override fun getStillFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_STILL
                "hot_coolant" -> HOT_COOLANT_STILL
                "uu_matter" -> UU_MATTER_STILL
                "weed_ex" -> WEED_EX_STILL
                "pahoehoe_lava" -> PAHOEHOE_LAVA_STILL
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getFlowingFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_FLOWING
                "hot_coolant" -> HOT_COOLANT_FLOWING
                "uu_matter" -> UU_MATTER_FLOWING
                "weed_ex" -> WEED_EX_FLOWING
                "pahoehoe_lava" -> PAHOEHOE_LAVA_FLOWING
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBlock(): Block = when (name) {
                "coolant" -> COOLANT_BLOCK
                "hot_coolant" -> HOT_COOLANT_BLOCK
                "uu_matter" -> UU_MATTER_BLOCK
                "weed_ex" -> WEED_EX_BLOCK
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BLOCK
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBucket(): Item = when (name) {
                "coolant" -> COOLANT_BUCKET
                "hot_coolant" -> HOT_COOLANT_BUCKET
                "uu_matter" -> UU_MATTER_BUCKET
                "weed_ex" -> WEED_EX_BUCKET
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BUCKET
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
        }
    }
}

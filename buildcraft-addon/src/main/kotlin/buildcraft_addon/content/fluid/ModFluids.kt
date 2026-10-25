package buildcraft_addon.content.fluid

import buildcraft_addon.BuildCraftAddon
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.resources.Identifier
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader

/**
 * BC 所有流体注册。
 *
 * 贴图统一使用 IC2 通用流体纹理 + 着色器染色。
 * 密度/粘度参考 BC 原版 BCEnergyFluids.java 表格。
 */
object ModFluids {

    const val TEXTURE_NS = "ic2"

    // ===== 原油 =====
    lateinit var CRUDE_OIL_STILL: FlowingFluid
    lateinit var CRUDE_OIL_FLOWING: FlowingFluid
    lateinit var CRUDE_OIL_BLOCK: Block

    // ===== 油渣 =====
    lateinit var OIL_RESIDUE_STILL: FlowingFluid
    lateinit var OIL_RESIDUE_FLOWING: FlowingFluid
    lateinit var OIL_RESIDUE_BLOCK: Block

    // ===== 重油 =====
    lateinit var HEAVY_OIL_STILL: FlowingFluid
    lateinit var HEAVY_OIL_FLOWING: FlowingFluid
    lateinit var HEAVY_OIL_BLOCK: Block

    // ===== 稠油 =====
    lateinit var DENSE_OIL_STILL: FlowingFluid
    lateinit var DENSE_OIL_FLOWING: FlowingFluid
    lateinit var DENSE_OIL_BLOCK: Block

    // ===== 蒸馏油 =====
    lateinit var DISTILLED_OIL_STILL: FlowingFluid
    lateinit var DISTILLED_OIL_FLOWING: FlowingFluid
    lateinit var DISTILLED_OIL_BLOCK: Block

    // ===== 稠燃油 =====
    lateinit var DENSE_FUEL_STILL: FlowingFluid
    lateinit var DENSE_FUEL_FLOWING: FlowingFluid
    lateinit var DENSE_FUEL_BLOCK: Block

    // ===== 混合重燃油 =====
    lateinit var MIXED_HEAVY_FUEL_STILL: FlowingFluid
    lateinit var MIXED_HEAVY_FUEL_FLOWING: FlowingFluid
    lateinit var MIXED_HEAVY_FUEL_BLOCK: Block

    // ===== 轻燃油 =====
    lateinit var LIGHT_FUEL_STILL: FlowingFluid
    lateinit var LIGHT_FUEL_FLOWING: FlowingFluid
    lateinit var LIGHT_FUEL_BLOCK: Block

    // ===== 混合轻燃油 =====
    lateinit var MIXED_LIGHT_FUEL_STILL: FlowingFluid
    lateinit var MIXED_LIGHT_FUEL_FLOWING: FlowingFluid
    lateinit var MIXED_LIGHT_FUEL_BLOCK: Block

    // ===== 气态燃油 =====
    lateinit var GASEOUS_FUEL_STILL: FlowingFluid
    lateinit var GASEOUS_FUEL_FLOWING: FlowingFluid
    lateinit var GASEOUS_FUEL_BLOCK: Block

    /** 服务端安全的 ARGB 颜色映射 */
    val fluidTintColors = mutableMapOf<Fluid, Int>()

    fun register() {
        // BC 原版流体数据 (密度, 粘度, 扩散, 颜色)
        // 颜色来自 BCEnergyFluids.java tex_light 值
        registerFluid("crude_oil",       900,  2000,  6, 0xFF505050.toInt())
        registerFluid("oil_residue",    1200,  4000,  4, 0xFF100F10.toInt())
        registerFluid("heavy_oil",       850,  1800,  6, 0xFFA08F1F.toInt())
        registerFluid("dense_oil",       950,  1600,  5, 0xFF876E77.toInt())
        registerFluid("distilled_oil",   750,  1400,  8, 0xFFE4AF78.toInt())
        registerFluid("dense_fuel",      600,   800,  7, 0xFFFFAF3F.toInt())
        registerFluid("mixed_heavy_fuel",700,  1000,  7, 0xFFF2A700.toInt())
        registerFluid("light_fuel",      400,   600,  8, 0xFFFFFF30.toInt())
        registerFluid("mixed_light_fuel",650,   900,  9, 0xFFF6D700.toInt())
        registerFluid("gaseous_fuel",    300,   500, 10, 0xFFFAF630.toInt())
    }

    private fun registerFluid(name: String, density: Int, viscosity: Int, spread: Int, colorArgb: Int) {
        val modId = BuildCraftAddon.MOD_ID

        // 1. Still / Flowing 流体
        val still = Registry.register(
            BuiltInRegistries.FLUID, BuildCraftAddon.id(name),
            BcOilFluid.Still(name)
        )
        val flowing = Registry.register(
            BuiltInRegistries.FLUID, BuildCraftAddon.id("flowing_$name"),
            BcOilFluid.Flowing(name)
        )

        BcOilFluid.fluidLookup[name] = Pair(still, flowing)

        // 存储染色颜色
        fluidTintColors[still] = colorArgb
        fluidTintColors[flowing] = colorArgb

        // 2. LiquidBlock
        val blockSettings = BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
            .noCollision().noLootTable().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, BuildCraftAddon.id(name)))
        val block = Registry.register(
            BuiltInRegistries.BLOCK, BuildCraftAddon.id(name),
            LiquidBlock(still, blockSettings)
        )

        // 3. 注册桶
        val bucket = Registry.register(
            BuiltInRegistries.ITEM, BuildCraftAddon.id("${name}_bucket"),
            BucketItem(still, Item.Properties().stacksTo(1).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, BuildCraftAddon.id("${name}_bucket"))))
        )
        BcOilFluid.bucketLookup[name] = bucket

        when (name) {
            "crude_oil" -> { CRUDE_OIL_STILL = still; CRUDE_OIL_FLOWING = flowing; CRUDE_OIL_BLOCK = block }
            "oil_residue" -> { OIL_RESIDUE_STILL = still; OIL_RESIDUE_FLOWING = flowing; OIL_RESIDUE_BLOCK = block }
            "heavy_oil" -> { HEAVY_OIL_STILL = still; HEAVY_OIL_FLOWING = flowing; HEAVY_OIL_BLOCK = block }
            "dense_oil" -> { DENSE_OIL_STILL = still; DENSE_OIL_FLOWING = flowing; DENSE_OIL_BLOCK = block }
            "distilled_oil" -> { DISTILLED_OIL_STILL = still; DISTILLED_OIL_FLOWING = flowing; DISTILLED_OIL_BLOCK = block }
            "dense_fuel" -> { DENSE_FUEL_STILL = still; DENSE_FUEL_FLOWING = flowing; DENSE_FUEL_BLOCK = block }
            "mixed_heavy_fuel" -> { MIXED_HEAVY_FUEL_STILL = still; MIXED_HEAVY_FUEL_FLOWING = flowing; MIXED_HEAVY_FUEL_BLOCK = block }
            "light_fuel" -> { LIGHT_FUEL_STILL = still; LIGHT_FUEL_FLOWING = flowing; LIGHT_FUEL_BLOCK = block }
            "mixed_light_fuel" -> { MIXED_LIGHT_FUEL_STILL = still; MIXED_LIGHT_FUEL_FLOWING = flowing; MIXED_LIGHT_FUEL_BLOCK = block }
            "gaseous_fuel" -> { GASEOUS_FUEL_STILL = still; GASEOUS_FUEL_FLOWING = flowing; GASEOUS_FUEL_BLOCK = block }
        }

        BuildCraftAddon.LOGGER.debug("Registered fluid: {} (d={}, v={})", name, density, viscosity)
    }

    // ========== BC 流体基类 ==========

    abstract class BcOilFluid : FlowingFluid() {

        companion object {
            val fluidLookup = mutableMapOf<String, Pair<Fluid, Fluid>>()
            val bucketLookup = mutableMapOf<String, Item>()
        }

        protected abstract val fluidName: String

        private val ref: Pair<Fluid, Fluid> get() = fluidLookup[fluidName] ?: error("Fluid $fluidName not registered")

        override fun getSource(): Fluid = ref.first
        override fun getFlowing(): Fluid = ref.second
        override fun getBucket(): Item = bucketLookup[fluidName] ?: Items.AIR
        override fun createLegacyBlock(state: FluidState): BlockState {
            val block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(BuildCraftAddon.MOD_ID, fluidName))
            return block.defaultBlockState().setValue(LiquidBlock.LEVEL, getAmount(state))
        }
        override fun isSame(fluid: Fluid): Boolean = fluid == ref.first || fluid == ref.second
        override fun canConvertToSource(world: net.minecraft.server.level.ServerLevel): Boolean = false
        override fun beforeDestroyingBlock(world: net.minecraft.world.level.LevelAccessor, pos: net.minecraft.core.BlockPos, state: BlockState) {
            val entity = if (state.hasBlockEntity()) world.getBlockEntity(pos) else null
            Block.dropResources(state, world, pos, entity)
        }
        override fun canBeReplacedWith(state: FluidState, world: BlockGetter, pos: net.minecraft.core.BlockPos, fluid: Fluid, direction: net.minecraft.core.Direction): Boolean = false
        override fun getSlopeFindDistance(world: LevelReader): Int = 4
        override fun getDropOff(world: LevelReader): Int = 1
        override fun getTickDelay(world: LevelReader): Int = 5
        override fun getExplosionResistance(): Float = 100f

        class Still(override val fluidName: String) : BcOilFluid() {
            override fun getAmount(state: FluidState): Int = 8
            override fun isSource(state: FluidState): Boolean = true
        }

        class Flowing(override val fluidName: String) : BcOilFluid() {
            override fun createFluidStateDefinition(builder: StateDefinition.Builder<Fluid, FluidState>) {
                super.createFluidStateDefinition(builder); builder.add(LEVEL)
            }
            override fun getAmount(state: FluidState): Int = state.getValue(LEVEL)
            override fun isSource(state: FluidState): Boolean = false
        }
    }
}

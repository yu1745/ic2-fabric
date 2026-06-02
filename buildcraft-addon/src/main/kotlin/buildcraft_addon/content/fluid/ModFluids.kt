package buildcraft_addon.content.fluid

import buildcraft_addon.BuildCraftAddon
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FluidBlock
import net.minecraft.fluid.FlowableFluid
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.state.StateManager
import net.minecraft.util.Identifier
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldView

/**
 * BC 所有流体注册。
 *
 * 贴图统一使用 IC2 通用流体纹理 + 着色器染色。
 * 密度/粘度参考 BC 原版 BCEnergyFluids.java 表格。
 */
object ModFluids {

    const val TEXTURE_NS = "ic2"

    // ===== 原油 =====
    lateinit var CRUDE_OIL_STILL: FlowableFluid
    lateinit var CRUDE_OIL_FLOWING: FlowableFluid
    lateinit var CRUDE_OIL_BLOCK: Block

    // ===== 油渣 =====
    lateinit var OIL_RESIDUE_STILL: FlowableFluid
    lateinit var OIL_RESIDUE_FLOWING: FlowableFluid
    lateinit var OIL_RESIDUE_BLOCK: Block

    // ===== 重油 =====
    lateinit var HEAVY_OIL_STILL: FlowableFluid
    lateinit var HEAVY_OIL_FLOWING: FlowableFluid
    lateinit var HEAVY_OIL_BLOCK: Block

    // ===== 稠油 =====
    lateinit var DENSE_OIL_STILL: FlowableFluid
    lateinit var DENSE_OIL_FLOWING: FlowableFluid
    lateinit var DENSE_OIL_BLOCK: Block

    // ===== 蒸馏油 =====
    lateinit var DISTILLED_OIL_STILL: FlowableFluid
    lateinit var DISTILLED_OIL_FLOWING: FlowableFluid
    lateinit var DISTILLED_OIL_BLOCK: Block

    // ===== 稠燃油 =====
    lateinit var DENSE_FUEL_STILL: FlowableFluid
    lateinit var DENSE_FUEL_FLOWING: FlowableFluid
    lateinit var DENSE_FUEL_BLOCK: Block

    // ===== 混合重燃油 =====
    lateinit var MIXED_HEAVY_FUEL_STILL: FlowableFluid
    lateinit var MIXED_HEAVY_FUEL_FLOWING: FlowableFluid
    lateinit var MIXED_HEAVY_FUEL_BLOCK: Block

    // ===== 轻燃油 =====
    lateinit var LIGHT_FUEL_STILL: FlowableFluid
    lateinit var LIGHT_FUEL_FLOWING: FlowableFluid
    lateinit var LIGHT_FUEL_BLOCK: Block

    // ===== 混合轻燃油 =====
    lateinit var MIXED_LIGHT_FUEL_STILL: FlowableFluid
    lateinit var MIXED_LIGHT_FUEL_FLOWING: FlowableFluid
    lateinit var MIXED_LIGHT_FUEL_BLOCK: Block

    // ===== 气态燃油 =====
    lateinit var GASEOUS_FUEL_STILL: FlowableFluid
    lateinit var GASEOUS_FUEL_FLOWING: FlowableFluid
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
            Registries.FLUID, BuildCraftAddon.id(name),
            BcOilFluid.Still(name)
        )
        val flowing = Registry.register(
            Registries.FLUID, BuildCraftAddon.id("flowing_$name"),
            BcOilFluid.Flowing(name)
        )

        BcOilFluid.fluidLookup[name] = Pair(still, flowing)

        // 存储染色颜色
        fluidTintColors[still] = colorArgb
        fluidTintColors[flowing] = colorArgb

        // 2. FluidBlock
        val blockSettings = AbstractBlock.Settings.copy(Blocks.WATER)
            .noCollision().dropsNothing()
        val block = Registry.register(
            Registries.BLOCK, BuildCraftAddon.id(name),
            FluidBlock(still, blockSettings)
        )

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

    abstract class BcOilFluid : FlowableFluid() {

        companion object {
            val fluidLookup = mutableMapOf<String, Pair<Fluid, Fluid>>()
        }

        protected abstract val fluidName: String

        private val ref: Pair<Fluid, Fluid> get() = fluidLookup[fluidName] ?: error("Fluid $fluidName not registered")

        override fun getStill(): Fluid = ref.first
        override fun getFlowing(): Fluid = ref.second
        override fun getBucketItem(): Item = Items.AIR
        override fun toBlockState(state: FluidState): BlockState {
            val block = Registries.BLOCK.get(Identifier.of(BuildCraftAddon.MOD_ID, fluidName))
            return block.defaultState.with(FluidBlock.LEVEL, getBlockStateLevel(state))
        }
        override fun matchesType(fluid: Fluid): Boolean = fluid == ref.first || fluid == ref.second
        override fun isInfinite(world: World): Boolean = false
        override fun beforeBreakingBlock(world: net.minecraft.world.WorldAccess, pos: net.minecraft.util.math.BlockPos, state: BlockState) {
            val entity = if (state.hasBlockEntity()) world.getBlockEntity(pos) else null
            Block.dropStacks(state, world, pos, entity)
        }
        override fun canBeReplacedWith(state: FluidState, world: BlockView, pos: net.minecraft.util.math.BlockPos, fluid: Fluid, direction: net.minecraft.util.math.Direction): Boolean = false
        override fun getLevelDecreasePerBlock(world: WorldView): Int = 1
        override fun getTickRate(world: WorldView): Int = 5
        override fun getBlastResistance(): Float = 100f

        class Still(override val fluidName: String) : BcOilFluid() {
            override fun getMaxFlowDistance(world: WorldView): Int = 4
            override fun getLevel(state: FluidState): Int = 8
            override fun isStill(state: FluidState): Boolean = true
        }

        class Flowing(override val fluidName: String) : BcOilFluid() {
            override fun getMaxFlowDistance(world: WorldView): Int = 4
            override fun appendProperties(builder: StateManager.Builder<Fluid, FluidState>) {
                super.appendProperties(builder); builder.add(LEVEL)
            }
            override fun getLevel(state: FluidState): Int = state.get(LEVEL)
            override fun isStill(state: FluidState): Boolean = false
        }
    }
}

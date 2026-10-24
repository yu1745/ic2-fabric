package buildcraft_addon.client

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.fluid.ModFluids
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler
import net.minecraft.fluid.Fluid
import net.minecraft.util.Identifier

/**
 * BC 流体客户端渲染注册。
 *
 * 流体颜色通过反射注入 IC2 的 fluidTintColors，让 IC2 通用流体单元也能正确染色。
 */
object ModFluidClient {

    private val STILL_TEX = Identifier.of("ic2", "block/fluid/fluid_still")
    private val FLOW_TEX = Identifier.of("ic2", "block/fluid/fluid_flow")

    fun register() {
        val fluids = listOf(
            ModFluids.CRUDE_OIL_STILL to ModFluids.CRUDE_OIL_FLOWING,
            ModFluids.OIL_RESIDUE_STILL to ModFluids.OIL_RESIDUE_FLOWING,
            ModFluids.HEAVY_OIL_STILL to ModFluids.HEAVY_OIL_FLOWING,
            ModFluids.DENSE_OIL_STILL to ModFluids.DENSE_OIL_FLOWING,
            ModFluids.DISTILLED_OIL_STILL to ModFluids.DISTILLED_OIL_FLOWING,
            ModFluids.DENSE_FUEL_STILL to ModFluids.DENSE_FUEL_FLOWING,
            ModFluids.MIXED_HEAVY_FUEL_STILL to ModFluids.MIXED_HEAVY_FUEL_FLOWING,
            ModFluids.LIGHT_FUEL_STILL to ModFluids.LIGHT_FUEL_FLOWING,
            ModFluids.MIXED_LIGHT_FUEL_STILL to ModFluids.MIXED_LIGHT_FUEL_FLOWING,
            ModFluids.GASEOUS_FUEL_STILL to ModFluids.GASEOUS_FUEL_FLOWING,
        )

        for ((still, flowing) in fluids) {
            val tint = ModFluids.fluidTintColors[still] ?: continue

            // 注册流体渲染（含染色）
            FluidRenderHandlerRegistry.INSTANCE.register(
                still, flowing,
                SimpleFluidRenderHandler(STILL_TEX, FLOW_TEX, tint)
            )
        }

        // 反射注入 IC2 的 fluidTintColors，让 IC2 通用流体单元能取到颜色
        injectTintToIc2()
    }

    private fun injectTintToIc2() {
        try {
            val ic2ModFluidsClass = Class.forName("ic2_120.content.fluid.ModFluids")
            val field = ic2ModFluidsClass.declaredFields.find { it.name == "fluidTintColors" }
                ?: run { BuildCraftAddon.LOGGER.warn("无法找到 IC2 fluidTintColors 字段"); return }
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val map = field.get(null) as? MutableMap<Any, Any>
                ?: run { BuildCraftAddon.LOGGER.warn("IC2 fluidTintColors 不是 MutableMap"); return }
            for ((fluid, color) in ModFluids.fluidTintColors) {
                map[fluid] = color
            }
            BuildCraftAddon.LOGGER.info("已向 IC2 fluidTintColors 注入 {} 种流体染色", ModFluids.fluidTintColors.size)
        } catch (e: Exception) {
            BuildCraftAddon.LOGGER.error("注入 IC2 fluidTintColors 失败", e)
        }
    }
}

package ic2_120.client

import ic2_120.content.fluid.ModFluids
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

/**
 * 客户端流体渲染注册。
 * 为 IC2 流体注册纹理与半透明渲染层。
 */
object ModFluidClient {

    private const val TEXTURE_NS = "ic2"
    private val logger = LoggerFactory.getLogger("ic2_120/ModFluidClient")

    fun register() {
        // Coolant
        registerFluid(
            ModFluids.COOLANT_STILL,
            ModFluids.COOLANT_FLOWING,
            "block/fluid/coolant_still",
            "block/fluid/coolant_flow"
        )
        // Hot Coolant
        registerFluid(
            ModFluids.HOT_COOLANT_STILL,
            ModFluids.HOT_COOLANT_FLOWING,
            "block/fluid/hot_coolant_still",
            "block/fluid/hot_coolant_flow"
        )
        // UU Matter
        registerFluid(
            ModFluids.UU_MATTER_STILL,
            ModFluids.UU_MATTER_FLOWING,
            "block/fluid/uu_matter_still",
            "block/fluid/uu_matter_flow"
        )
        // Weed-EX（仅 still 纹理，flow 复用 still）
        registerFluid(
            ModFluids.WEED_EX_STILL,
            ModFluids.WEED_EX_FLOWING,
            "block/fluid/weed_ex_still",
            "block/fluid/weed_ex_still"
        )
        // Pahoehoe Lava（仅 still 纹理，flow 复用 still）
        registerFluid(
            ModFluids.PAHOEHOE_LAVA_STILL,
            ModFluids.PAHOEHOE_LAVA_FLOWING,
            "block/fluid/pahoehoe_lava_still",
            "block/fluid/pahoehoe_lava_still"
        )
        // Distilled Water（渲染复用原版水）
        registerFluid(
            ModFluids.DISTILLED_WATER_STILL,
            ModFluids.DISTILLED_WATER_FLOWING,
            "minecraft:block/water_still",
            "minecraft:block/water_flow"
            //不传颜色，使其渲染为白灰色，但是实际上没啥用，因为放出来会自动变成水，不用指令创建不了这个蒸馏水方块
        )
        // Biofuel（151, 202, 0）
        registerFluid(
            ModFluids.BIOFUEL_STILL,
            ModFluids.BIOFUEL_FLOWING,
            "block/fluid/fluid_still",
            "block/fluid/fluid_flow",
            rgb(151, 202, 0)
        )
        // Biomass（71, 106, 60）
        registerFluid(
            ModFluids.BIOMASS_STILL,
            ModFluids.BIOMASS_FLOWING,
            "block/fluid/fluid_still",
            "block/fluid/fluid_flow",
            rgb(71, 106, 60)
        )
        // Construction foam（浅灰，与 FluidUtils.getFluidColor 一致）
        registerFluid(
            ModFluids.CONSTRUCTION_FOAM_STILL,
            ModFluids.CONSTRUCTION_FOAM_FLOWING,
            "block/fluid/fluid_still",
            "block/fluid/fluid_flow",
            rgb(180, 180, 175)
        )

        // 流体方块使用半透明渲染层
        BlockRenderLayerMap.INSTANCE.putFluids(
            RenderLayer.getTranslucent(),
            ModFluids.COOLANT_STILL,
            ModFluids.COOLANT_FLOWING,
            ModFluids.HOT_COOLANT_STILL,
            ModFluids.HOT_COOLANT_FLOWING,
            ModFluids.UU_MATTER_STILL,
            ModFluids.UU_MATTER_FLOWING,
            ModFluids.WEED_EX_STILL,
            ModFluids.WEED_EX_FLOWING,
            ModFluids.PAHOEHOE_LAVA_STILL,
            ModFluids.PAHOEHOE_LAVA_FLOWING,
            ModFluids.DISTILLED_WATER_STILL,
            ModFluids.DISTILLED_WATER_FLOWING,
            ModFluids.BIOFUEL_STILL,
            ModFluids.BIOFUEL_FLOWING,
            ModFluids.BIOMASS_STILL,
            ModFluids.BIOMASS_FLOWING,
            ModFluids.CONSTRUCTION_FOAM_STILL,
            ModFluids.CONSTRUCTION_FOAM_FLOWING
        )
    }

    private fun registerFluid(
        still: net.minecraft.fluid.Fluid,
        flowing: net.minecraft.fluid.Fluid,
        stillTex: String,
        flowTex: String
    ) {
        val stillId = parseTextureId(stillTex)
        val flowId = parseTextureId(flowTex)
        logFluidRegistration(still, flowing, stillId, flowId, null)
        FluidRenderHandlerRegistry.INSTANCE.register(still, flowing, SimpleFluidRenderHandler(stillId, flowId))
    }

    private fun registerFluid(
        still: net.minecraft.fluid.Fluid,
        flowing: net.minecraft.fluid.Fluid,
        stillTex: String,
        flowTex: String,
        tintColor: Int
    ) {
        val stillId = parseTextureId(stillTex)
        val flowId = parseTextureId(flowTex)
        logFluidRegistration(still, flowing, stillId, flowId, tintColor)
        FluidRenderHandlerRegistry.INSTANCE.register(
            still,
            flowing,
            SimpleFluidRenderHandler(stillId, flowId, tintColor)
        )
    }

    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun parseTextureId(path: String): Identifier {
        return if (path.contains(':')) {
            Identifier.tryParse(path) ?: Identifier(TEXTURE_NS, path)
        } else {
            Identifier(TEXTURE_NS, path)
        }
    }

    private fun logFluidRegistration(
        stillFluid: net.minecraft.fluid.Fluid,
        flowingFluid: net.minecraft.fluid.Fluid,
        stillId: Identifier,
        flowId: Identifier,
        tintColor: Int?
    ) {
        val stillPath = "/assets/${stillId.namespace}/textures/${stillId.path}.png"
        val flowPath = "/assets/${flowId.namespace}/textures/${flowId.path}.png"
        val stillExists = javaClass.getResource(stillPath) != null
        val flowExists = javaClass.getResource(flowPath) != null
        val tintInfo = tintColor?.let { String.format("#%08X", it) } ?: "<none>"
        // logger.info(
        //     "Register fluid render: stillFluid={}, flowFluid={}, stillTex={}, flowTex={}, tint={}, stillExists={}, flowExists={}",
        //     stillFluid, flowingFluid, stillId, flowId, tintInfo, stillExists, flowExists
        // )
        if (!stillExists || !flowExists) {
            logger.warn("Missing fluid texture resource: stillPath={}, flowPath={}", stillPath, flowPath)
        }
    }
}

package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.fluid.ModFluids
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

/**
 * 客户端流体渲染注册。
 * 为 IC2 流体注册纹理与半透明渲染层。
 */
object ModFluidClient {

    private const val TEXTURE_NS = "ic2"

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
            ModFluids.PAHOEHOE_LAVA_FLOWING
        )
    }

    private fun registerFluid(
        still: net.minecraft.fluid.Fluid,
        flowing: net.minecraft.fluid.Fluid,
        stillTex: String,
        flowTex: String
    ) {
        val stillId = Identifier(TEXTURE_NS, stillTex)
        val flowId = Identifier(TEXTURE_NS, flowTex)
        FluidRenderHandlerRegistry.INSTANCE.register(still, flowing, SimpleFluidRenderHandler(stillId, flowId))
    }
}

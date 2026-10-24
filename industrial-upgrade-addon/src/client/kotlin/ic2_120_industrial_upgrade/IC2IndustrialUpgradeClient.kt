package ic2_120_industrial_upgrade

import ic2_120.client.ClientScreenRegistrar
import ic2_120.content.fluid.ModFluids
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import ic2_120_industrial_upgrade.content.fluid.NeutronFluid
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

object IC2IndustrialUpgradeClient : ClientModInitializer {
    override fun onInitializeClient() {
        // 1. 扫描 @ModScreen 注解类，注册到 HandledScreens（中子制造机 GUI 等）
        ClientScreenRegistrar.registerScreens(
            IC2IndustrialUpgrade.MOD_ID,
            listOf("ic2_120_industrial_upgrade.client.screen")
        )

        // 2. 注册中子流体的客户端渲染（贴图 + 半透明渲染层）
        // 对齐 core 模式 B（construction_foam/creosote/compressed_air）：无独立 PNG，
        // 引用 core 的 ic2:block/fluid/fluid_still / fluid_flow 通用纹理 + tint 着色。
        // 避免 still/flow 两张独立渐变图在连接处出现断裂（见 NeutronFluid.kt 注释）。
        val tint = ModFluids.getFluidTintOrNull(NeutronFluid.NEUTRON_STILL) ?: 0xFF8B7FD4.toInt()
        FluidRenderHandlerRegistry.INSTANCE.register(
            NeutronFluid.NEUTRON_STILL,
            NeutronFluid.NEUTRON_FLOWING,
            SimpleFluidRenderHandler(
                ResourceLocation.of("ic2", "block/fluid/fluid_still"),
                ResourceLocation.of("ic2", "block/fluid/fluid_flow"),
                tint
            )
        )
        BlockRenderLayerMap.INSTANCE.putFluids(
            RenderType.getTranslucent(),
            NeutronFluid.NEUTRON_STILL,
            NeutronFluid.NEUTRON_FLOWING
        )
    }
}

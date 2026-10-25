package ic2_120_advanced_solar_addon.client

import ic2_120.client.ClientScreenRegistrar
import ic2_120.content.network.ChunkedConfigReceiver
import ic2_120.content.network.ConfigSyncPacket
import ic2_120.registry.type
import ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon
import ic2_120_advanced_solar_addon.client.render.MolecularTransformerBlockEntityRenderer
import ic2_120_advanced_solar_addon.client.render.MolecularTransformerItemSpecialRenderer
import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlockEntity
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120_advanced_solar_addon.integration.jei.Ic2AdvancedSolarAddonJeiPlugin
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import ic2_120.content.network.ConfigSyncChunkPayload

object IC2AdvancedSolarAddonClient : ClientModInitializer {
    private val configReceiver = ChunkedConfigReceiver { json ->
        Ic2AdvancedSolarAddonConfig.applyServerConfig(json)
        MTRecipes.loadFromConfig()
        if (FabricLoader.getInstance().isModLoaded("jei")) {
            Ic2AdvancedSolarAddonJeiPlugin.refreshMTRecipes()
        }
    }

    override fun onInitializeClient() {
        ClientScreenRegistrar.registerScreens(
            "ic2_120_advanced_solar_addon",
            listOf("ic2_120_advanced_solar_addon.client.screen")
        )

        // Register Block Entity Renderers
        val beType = MolecularTransformerBlockEntity::class.type()
        println("[MT-BER] Registering BER for type: $beType")
        BlockEntityRenderers.register(
            beType,
            ::MolecularTransformerBlockEntityRenderer
        )
        println("[MT-BER] Registration complete")

        MolecularTransformerItemSpecialRenderer.register()

        // 注册配置同步接收（分包）
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncChunkPayload.ID) { payload, _ ->
            val packet = ConfigSyncPacket(payload.totalChunks, payload.chunkIndex, payload.chunkData)
            Minecraft.getInstance().execute {
                configReceiver.accept(packet.totalChunks, packet.chunkIndex, packet.chunkData)
            }
        }
    }
}

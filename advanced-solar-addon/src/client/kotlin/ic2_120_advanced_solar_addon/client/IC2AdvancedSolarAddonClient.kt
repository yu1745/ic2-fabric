package ic2_120_advanced_solar_addon.client

import ic2_120_advanced_solar_addon.client.render.MolecularTransformerBlockEntityRenderer
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlockEntity
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import ic2_120.client.ClientScreenRegistrar
import ic2_120.registry.type

object IC2AdvancedSolarAddonClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientScreenRegistrar.registerScreens(
            "ic2_120_advanced_solar_addon",
            listOf("ic2_120_advanced_solar_addon.client.screen")
        )

        // Register Block Entity Renderers
        val beType = MolecularTransformerBlockEntity::class.type()
        println("[MT-BER] Registering BER for type: $beType")
        BlockEntityRendererFactories.register(
            beType,
            ::MolecularTransformerBlockEntityRenderer
        )
        println("[MT-BER] Registration complete")

        val mtBlock = Registries.BLOCK.get(Identifier.of("ic2_120_advanced_solar_addon", "molecular_transformer"))
        val mtItemEntity = MolecularTransformerBlockEntity(
            BlockPos.ORIGIN,
            mtBlock.defaultState.with(MolecularTransformerBlock.ACTIVE, false)
        )
        BuiltinItemRendererRegistry.INSTANCE.register(mtBlock.asItem()) { _, _, matrices, vertexConsumers, light, overlay ->
            MinecraftClient.getInstance().blockEntityRenderDispatcher.renderEntity(
                mtItemEntity,
                matrices,
                vertexConsumers,
                light,
                overlay
            )
        }
    }
}

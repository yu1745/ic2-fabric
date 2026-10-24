package buildcraft_addon.client

import buildcraft_addon.client.render.CreativeEngineRenderer
import buildcraft_addon.client.render.IronEngineRenderer
import buildcraft_addon.client.render.PumpRenderer
import buildcraft_addon.client.render.RFEngineRenderer
import buildcraft_addon.client.render.RedstoneEngineRenderer
import buildcraft_addon.client.render.StoneEngineRenderer
import buildcraft_addon.content.block.CreativeEngineBlock
import buildcraft_addon.content.block.IronEngineBlock
import buildcraft_addon.content.block.RFEngineBlock
import buildcraft_addon.content.block.RedstoneEngineBlock
import buildcraft_addon.content.block.StoneEngineBlock
import buildcraft_addon.content.blockentity.CreativeEngineBlockEntity
import buildcraft_addon.content.blockentity.IronEngineBlockEntity
import buildcraft_addon.content.blockentity.PumpBlockEntity
import buildcraft_addon.content.blockentity.RFEngineBlockEntity
import buildcraft_addon.content.blockentity.RedstoneEngineBlockEntity
import buildcraft_addon.content.blockentity.StoneEngineBlockEntity
import ic2_120.client.ClientScreenRegistrar
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.core.BlockPos

object BuildCraftAddonClient : ClientModInitializer {
    override fun onInitializeClient() {
        ModFluidClient.register()

        ClientScreenRegistrar.registerScreens(
            "buildcraft_addon",
            listOf("buildcraft_addon.client.screen")
        )

        BlockEntityRenderers.register(
            RedstoneEngineBlockEntity::class.type(),
            ::RedstoneEngineRenderer
        )
        BlockEntityRenderers.register(
            PumpBlockEntity::class.type(),
            ::PumpRenderer
        )
        BlockEntityRenderers.register(
            StoneEngineBlockEntity::class.type(),
            ::StoneEngineRenderer
        )
        BlockEntityRenderers.register(
            IronEngineBlockEntity::class.type(),
            ::IronEngineRenderer
        )
        BlockEntityRenderers.register(
            RFEngineBlockEntity::class.type(),
            ::RFEngineRenderer
        )
        BlockEntityRenderers.register(
            CreativeEngineBlockEntity::class.type(),
            ::CreativeEngineRenderer
        )

        // Register BER-based item rendering (dummy entities for inventory/ground rendering)
        val dummyRedstone = RedstoneEngineBlockEntity(BlockPos.ORIGIN, RedstoneEngineBlock::class.instance().defaultState)
        BuiltinItemRendererRegistry.INSTANCE.register(RedstoneEngineBlock::class.instance().asItem()) { _, _, matrices, vertexConsumers, light, overlay ->
            Minecraft.getInstance().blockEntityRenderDispatcher.renderEntity(dummyRedstone, matrices, vertexConsumers, light, overlay)
        }

        val dummyStone = StoneEngineBlockEntity(BlockPos.ORIGIN, StoneEngineBlock::class.instance().defaultState)
        BuiltinItemRendererRegistry.INSTANCE.register(StoneEngineBlock::class.instance().asItem()) { _, _, matrices, vertexConsumers, light, overlay ->
            Minecraft.getInstance().blockEntityRenderDispatcher.renderEntity(dummyStone, matrices, vertexConsumers, light, overlay)
        }

        val dummyIron = IronEngineBlockEntity(BlockPos.ORIGIN, IronEngineBlock::class.instance().defaultState)
        BuiltinItemRendererRegistry.INSTANCE.register(IronEngineBlock::class.instance().asItem()) { _, _, matrices, vertexConsumers, light, overlay ->
            Minecraft.getInstance().blockEntityRenderDispatcher.renderEntity(dummyIron, matrices, vertexConsumers, light, overlay)
        }

        val dummyRF = RFEngineBlockEntity(BlockPos.ORIGIN, RFEngineBlock::class.instance().defaultState)
        BuiltinItemRendererRegistry.INSTANCE.register(RFEngineBlock::class.instance().asItem()) { _, _, matrices, vertexConsumers, light, overlay ->
            Minecraft.getInstance().blockEntityRenderDispatcher.renderEntity(dummyRF, matrices, vertexConsumers, light, overlay)
        }

        val dummyCreative = CreativeEngineBlockEntity(BlockPos.ORIGIN, CreativeEngineBlock::class.instance().defaultState)
        BuiltinItemRendererRegistry.INSTANCE.register(CreativeEngineBlock::class.instance().asItem()) { _, _, matrices, vertexConsumers, light, overlay ->
            Minecraft.getInstance().blockEntityRenderDispatcher.renderEntity(dummyCreative, matrices, vertexConsumers, light, overlay)
        }
    }
}

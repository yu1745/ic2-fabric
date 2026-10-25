package buildcraft_addon.client

import buildcraft_addon.client.render.CreativeEngineRenderer
import buildcraft_addon.client.render.EngineItemSpecialRenderer
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
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.core.BlockPos

object BuildCraftAddonClient : ClientModInitializer {
    override fun onInitializeClient() {
        EngineItemSpecialRenderer.register()
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

    }
}

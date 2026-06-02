package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.CreativeEngineBlockEntity
import buildcraft_addon.content.blockentity.PowerStage
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier

class CreativeEngineRenderer(ctx: BlockEntityRendererFactory.Context) :
    BlockEntityRenderer<CreativeEngineBlockEntity> {

    private var spritesLoaded = false
    private lateinit var trunkBlack: Sprite
    private lateinit var backSprite: Sprite
    private lateinit var sideSprite: Sprite
    private lateinit var chamberSprite: Sprite

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = MinecraftClient.getInstance().bakedModelManager.getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
        fun sprite(path: String) = atlas.getSprite(Identifier.of(BuildCraftAddon.MOD_ID, path))
        trunkBlack = sprite("block/engine/trunk_black")
        backSprite = sprite("block/engine/creative/back")
        sideSprite = sprite("block/engine/creative/side")
        chamberSprite = sprite("block/engine/chamber_base")
        spritesLoaded = true
    }

    override fun render(
        entity: CreativeEngineBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: net.minecraft.client.render.VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        ensureSprites()
        val buffer = vertexConsumers.getBuffer(RenderLayer.getCutout())
        EngineRenderHelper.renderEngine(
            matrices = matrices,
            buffer = buffer,
            light = light,
            overlay = overlay,
            progress = entity.progress,
            stage = PowerStage.BLACK,
            facing = entity.cachedState.get(net.minecraft.state.property.Properties.FACING),
            world = entity.world,
            pos = entity.pos,
            backSprite = backSprite,
            sideSprite = sideSprite,
            trunkSprite = trunkBlack,
            chamberSprite = chamberSprite
        )
    }

    override fun rendersOutsideBoundingBox(entity: CreativeEngineBlockEntity): Boolean = true
}

package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.PowerStage
import buildcraft_addon.content.blockentity.RFEngineBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier

class RFEngineRenderer(ctx: BlockEntityRendererFactory.Context) :
    BlockEntityRenderer<RFEngineBlockEntity> {

    private var spritesLoaded = false
    private lateinit var trunkBlue: Sprite
    private lateinit var trunkGreen: Sprite
    private lateinit var trunkYellow: Sprite
    private lateinit var trunkRed: Sprite
    private lateinit var trunkOverheat: Sprite
    private lateinit var backSprite: Sprite
    private lateinit var sideSprite: Sprite
    private lateinit var chamberSprite: Sprite

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = MinecraftClient.getInstance().bakedModelManager.getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
        fun sprite(path: String) = atlas.getSprite(Identifier.of(BuildCraftAddon.MOD_ID, path))
        trunkBlue = sprite("block/engine/trunk_blue")
        trunkGreen = sprite("block/engine/trunk_green")
        trunkYellow = sprite("block/engine/trunk_yellow")
        trunkRed = sprite("block/engine/trunk_red")
        trunkOverheat = sprite("block/engine/trunk_overheat")
        backSprite = sprite("block/engine/rf/back")
        sideSprite = sprite("block/engine/rf/side")
        chamberSprite = sprite("block/engine/chamber_base")
        spritesLoaded = true
    }

    private fun getTrunkSprite(stage: PowerStage): Sprite = when (stage) {
        PowerStage.BLUE -> trunkBlue
        PowerStage.GREEN -> trunkGreen
        PowerStage.YELLOW -> trunkYellow
        PowerStage.RED -> trunkRed
        PowerStage.OVERHEAT -> trunkOverheat
        PowerStage.BLACK -> trunkBlue
    }

    override fun render(
        entity: RFEngineBlockEntity,
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
            stage = entity.currentStage,
            facing = entity.cachedState.get(net.minecraft.state.property.Properties.FACING),
            world = entity.world,
            pos = entity.pos,
            backSprite = backSprite,
            sideSprite = sideSprite,
            trunkSprite = getTrunkSprite(entity.currentStage),
            chamberSprite = chamberSprite
        )
    }

    override fun rendersOutsideBoundingBox(entity: RFEngineBlockEntity): Boolean = true
}

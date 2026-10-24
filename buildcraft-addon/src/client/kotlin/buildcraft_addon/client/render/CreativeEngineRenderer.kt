package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.CreativeEngineBlockEntity
import buildcraft_addon.content.blockentity.PowerStage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.resources.ResourceLocation

class CreativeEngineRenderer(ctx: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<CreativeEngineBlockEntity> {

    private var spritesLoaded = false
    private lateinit var trunkBlack: TextureAtlasSprite
    private lateinit var backSprite: TextureAtlasSprite
    private lateinit var sideSprite: TextureAtlasSprite
    private lateinit var chamberSprite: TextureAtlasSprite

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = Minecraft.getInstance().bakedModelManager.getAtlas(InventoryMenu.BLOCK_ATLAS_TEXTURE)
        fun sprite(path: String) = atlas.getSprite(ResourceLocation(BuildCraftAddon.MOD_ID, path))
        trunkBlack = sprite("block/engine/trunk_black")
        backSprite = sprite("block/engine/creative/back")
        sideSprite = sprite("block/engine/creative/side")
        chamberSprite = sprite("block/engine/chamber_base")
        spritesLoaded = true
    }

    override fun render(
        entity: CreativeEngineBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: net.minecraft.client.renderer.MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        ensureSprites()
        val buffer = vertexConsumers.getBuffer(RenderType.getCutout())
        EngineRenderHelper.renderEngine(
            matrices = matrices,
            buffer = buffer,
            light = light,
            overlay = overlay,
            progress = entity.progress,
            stage = PowerStage.BLACK,
            facing = entity.cachedState.getValue(et.minecraft.world.level.block.state.properties.BlockStateProperties.FACING),
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

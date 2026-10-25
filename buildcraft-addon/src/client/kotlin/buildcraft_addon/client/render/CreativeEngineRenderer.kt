package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.CreativeEngineBlockEntity
import buildcraft_addon.content.blockentity.PowerStage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.data.AtlasIds
import ic2_120.client.renderers.LegacyBlockEntityRenderer26
import ic2_120.client.renderers.LegacyMultiBufferSource26
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.resources.Identifier
import ic2_120.compat.*

class CreativeEngineRenderer(ctx: BlockEntityRendererProvider.Context?) :
    LegacyBlockEntityRenderer26<CreativeEngineBlockEntity>() {

    private var spritesLoaded = false
    private lateinit var trunkBlack: TextureAtlasSprite
    private lateinit var backSprite: TextureAtlasSprite
    private lateinit var sideSprite: TextureAtlasSprite
    private lateinit var chamberSprite: TextureAtlasSprite

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = Minecraft.getInstance().atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)
        fun sprite(path: String) = atlas.getSprite(Identifier.fromNamespaceAndPath(BuildCraftAddon.MOD_ID, path))
        trunkBlack = sprite("block/engine/trunk_black")
        backSprite = sprite("block/engine/creative/back")
        sideSprite = sprite("block/engine/creative/side")
        chamberSprite = sprite("block/engine/chamber_base")
        spritesLoaded = true
    }

    override fun renderLegacy(
        entity: CreativeEngineBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: LegacyMultiBufferSource26,
        light: Int,
        overlay: Int
    ) {
        ensureSprites()
        val buffer = vertexConsumers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS))
        EngineRenderHelper.renderEngine(
            matrices = matrices,
            buffer = buffer,
            light = light,
            overlay = overlay,
            progress = entity.progress,
            stage = PowerStage.BLACK,
            facing = entity.cachedState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING),
            world = entity.world,
            pos = entity.pos,
            backSprite = backSprite,
            sideSprite = sideSprite,
            trunkSprite = trunkBlack,
            chamberSprite = chamberSprite
        )
    }

    override fun shouldRenderOffScreen(): Boolean = true
}

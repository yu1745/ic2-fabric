package buildcraft_addon.client.render

import buildcraft_addon.BuildCraftAddon
import buildcraft_addon.content.blockentity.IronEngineBlockEntity
import buildcraft_addon.content.blockentity.PowerStage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.resources.ResourceLocation

class IronEngineRenderer(ctx: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<IronEngineBlockEntity> {

    private var spritesLoaded = false
    private lateinit var trunkBlue: TextureAtlasSprite
    private lateinit var trunkGreen: TextureAtlasSprite
    private lateinit var trunkYellow: TextureAtlasSprite
    private lateinit var trunkRed: TextureAtlasSprite
    private lateinit var trunkOverheat: TextureAtlasSprite
    private lateinit var backSprite: TextureAtlasSprite
    private lateinit var sideSprite: TextureAtlasSprite
    private lateinit var chamberSprite: TextureAtlasSprite

    private fun ensureSprites() {
        if (spritesLoaded) return
        val atlas = Minecraft.getInstance().bakedModelManager.getAtlas(InventoryMenu.BLOCK_ATLAS_TEXTURE)
        fun sprite(path: String) = atlas.getSprite(ResourceLocation(BuildCraftAddon.MOD_ID, path))
        trunkBlue = sprite("block/engine/trunk_blue")
        trunkGreen = sprite("block/engine/trunk_green")
        trunkYellow = sprite("block/engine/trunk_yellow")
        trunkRed = sprite("block/engine/trunk_red")
        trunkOverheat = sprite("block/engine/trunk_overheat")
        backSprite = sprite("block/engine/iron/back")
        sideSprite = sprite("block/engine/iron/side")
        chamberSprite = sprite("block/engine/chamber_base")
        spritesLoaded = true
    }

    private fun getTrunkSprite(stage: PowerStage): TextureAtlasSprite = when (stage) {
        PowerStage.BLUE -> trunkBlue
        PowerStage.GREEN -> trunkGreen
        PowerStage.YELLOW -> trunkYellow
        PowerStage.RED -> trunkRed
        PowerStage.OVERHEAT -> trunkOverheat
        PowerStage.BLACK -> trunkBlue
    }

    override fun render(
        entity: IronEngineBlockEntity,
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
            stage = entity.currentStage,
            facing = entity.cachedState.get(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING),
            world = entity.world,
            pos = entity.pos,
            backSprite = backSprite,
            sideSprite = sideSprite,
            trunkSprite = getTrunkSprite(entity.currentStage),
            chamberSprite = chamberSprite
        )
    }

    override fun rendersOutsideBoundingBox(entity: IronEngineBlockEntity): Boolean = true
}

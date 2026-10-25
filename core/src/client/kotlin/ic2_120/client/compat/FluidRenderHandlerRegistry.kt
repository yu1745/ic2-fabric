package ic2_120.client.compat

import ic2_120.content.fluid.ModFluids
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids

/** Sprite and tint access for legacy GUI renderers using the 26.2 fluid model set. */
class FluidRenderHandlerRegistry private constructor() {
    companion object {
        val INSTANCE = FluidRenderHandlerRegistry()
    }

    fun get(fluid: Fluid): FluidSpriteHandler? =
        if (fluid == Fluids.EMPTY) null else FluidSpriteHandler(fluid)
}

class FluidSpriteHandler(private val fluid: Fluid) {
    fun getFluidSprites(world: Level?, pos: BlockPos?, state: FluidState): Array<TextureAtlasSprite> {
        val model = Minecraft.getInstance().modelManager.fluidStateModelSet.get(state)
        return arrayOf(model.stillMaterial().sprite(), model.flowingMaterial().sprite())
    }

    fun getFluidColor(world: Level?, pos: BlockPos?, state: FluidState): Int =
        ModFluids.getFluidTintOrNull(fluid) ?: -1
}

package buildcraft_addon.client.render

import net.minecraft.core.BlockPos
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.level.BlockAndLightGetter
import net.minecraft.world.level.LightLayer

internal fun lightCoords26(world: BlockAndLightGetter, pos: BlockPos): Int =
    LightCoordsUtil.pack(world.getBrightness(LightLayer.BLOCK, pos), world.getBrightness(LightLayer.SKY, pos))

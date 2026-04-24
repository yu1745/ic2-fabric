package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import ic2_120.client.FluidUtils
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.getFluidCellVariant
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.fluid.Fluids
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 通用流体单元着色器：从流体贴图采样颜色，渲染到物品中心（tintindex 1），
 * 便于手持时识别所含流体。
 *
 * 同时处理专用流体单元（如 weed_ex_cell），直接从其 getFluid() 获取流体颜色。
 */
object FluidCellColorProvider {

    fun register() {
        val fluidCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))
        ColorProviderRegistry.ITEM.register({ stack, tintIndex ->
            if (tintIndex != 1) return@register -1
            val fluid = stack.getFluidCellVariant()?.fluid ?: return@register -1
            FluidUtils.getFluidColor(fluid)
        }, fluidCell)

        // 专用流体单元：从 ModFluidCell.getFluid() 获取流体颜色
        val weedExCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "weed_ex_cell"))
        ColorProviderRegistry.ITEM.register({ stack, tintIndex ->
            if (tintIndex != 1) return@register -1
            val item = stack.item
            if (item !is ModFluidCell) return@register -1
            FluidUtils.getFluidColor(item.getFluid())
        }, weedExCell)
    }
}

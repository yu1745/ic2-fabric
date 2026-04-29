package ic2_120.content.block.pipes

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries

object PumpAttachmentFilter {
    fun resolveFluidIdFromStack(stack: ItemStack): String? {
        if (stack.isEmpty) return null
        val storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) ?: return null
        val view = storage.iterator().asSequence().firstOrNull { !it.resource.isBlank && it.amount > 0L } ?: return null
        return Registries.FLUID.getId(view.resource.fluid).toString()
    }
}

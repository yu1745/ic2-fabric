package ic2_120.content.upgrade

import ic2_120.content.item.FluidEjectorUpgrade
import ic2_120.content.item.FluidPullingUpgrade
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object FluidPipeUpgradeComponent {
    private const val NBT_FILTER = "PipeFluidFilter"

    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is IFluidPipeUpgradeSupport) return

        var provider = false
        var receiver = false
        var providerFilter: Fluid? = null
        var receiverFilter: Fluid? = null

        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (stack.isEmpty) continue
            when (stack.item) {
                is FluidEjectorUpgrade -> {
                    provider = true
                    if (providerFilter == null) providerFilter = readFilter(stack)
                }
                is FluidPullingUpgrade -> {
                    receiver = true
                    if (receiverFilter == null) receiverFilter = readFilter(stack)
                }
            }
        }

        machine.fluidPipeProviderEnabled = provider
        machine.fluidPipeReceiverEnabled = receiver
        machine.fluidPipeProviderFilter = providerFilter
        machine.fluidPipeReceiverFilter = receiverFilter
    }

    fun readFilter(stack: ItemStack): Fluid? {
        val nbt = stack.nbt ?: return null
        val raw = nbt.getString(NBT_FILTER)
        if (raw.isNullOrBlank()) return null
        val id = Identifier.tryParse(raw) ?: return null
        return if (Registries.FLUID.containsId(id)) Registries.FLUID.get(id) else null
    }

    fun writeFilter(stack: ItemStack, fluid: Fluid?) {
        val nbt = stack.orCreateNbt
        if (fluid == null) {
            nbt.remove(NBT_FILTER)
            return
        }
        val id = Registries.FLUID.getId(fluid)
        if (id.path != "empty") {
            nbt.putString(NBT_FILTER, id.toString())
        } else {
            nbt.remove(NBT_FILTER)
        }
    }
}

package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.recipes.ModTags
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.fluid.Fluid
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/** 从物品容器中读取半流质发电机可用燃料流体。 */
fun ItemStack.getSemifluidFuelFluid(): Fluid? {
    if (isEmpty) return null

    ModFluids.getFluidFromModBucket(item)?.let { return it }

    if (item == Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "biofuel_cell"))) {
        return ModFluids.BIOFUEL_STILL
    }

    if (item == Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))) {
        return getFluidCellVariant()?.fluid
    }

    val storage = FluidStorage.ITEM.find(this, ContainerItemContext.withConstant(this)) ?: return null
    val view = storage.iterator().asSequence().firstOrNull { !it.resource.isBlank && it.amount > 0L } ?: return null
    return view.resource.fluid
}

/** 判断物品是否为半流质发电机燃料。 */
fun ItemStack.isSemifluidFuel(): Boolean {
    val fluid = getSemifluidFuelFluid() ?: return false
    // Fluid.isIn(TagKey) 与 Fluid.getRegistryEntry() 在 1.20.1 均已 @Deprecated，
    // 改用未废弃的 FluidState.isIn（其内部委托 RegistryEntry.isIn）。
    val state = fluid.defaultState
    return state.isIn(ModTags.Compat.Fluids.SEMIFLUID_BIOFUEL_EQUIVALENT) ||
        state.isIn(ModTags.Compat.Fluids.SEMIFLUID_CREOSOTE_EQUIVALENT)
}

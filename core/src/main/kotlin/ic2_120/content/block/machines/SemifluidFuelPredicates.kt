package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.getFluidCellVariant
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/** 判断物品是否为半流质发电机燃料（生物燃料、杂酚油等）。 */
fun ItemStack.isSemifluidFuel(): Boolean {
    return when (item) {
        ModFluids.BIOFUEL_BUCKET -> true
        ModFluids.CREOSOTE_BUCKET -> true
        Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "biofuel_cell")) -> true
        Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell")) -> {
            val fluid = getFluidCellVariant()?.fluid ?: return false
            fluid == ModFluids.BIOFUEL_STILL || fluid == ModFluids.BIOFUEL_FLOWING ||
                fluid == ModFluids.CREOSOTE_STILL || fluid == ModFluids.CREOSOTE_FLOWING
        }
        else -> false
    }
}

package ic2_120

import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

fun ItemStack.getCustomData(): NbtCompound? =
    this.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt()

fun ItemStack.getOrCreateCustomData(): NbtCompound {
    val existing = this.get(DataComponentTypes.CUSTOM_DATA)
    val nbt = existing?.copyNbt() ?: NbtCompound()
    this.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
    return nbt
}

fun ItemStack.removeCustomData() {
    this.remove(DataComponentTypes.CUSTOM_DATA)
}

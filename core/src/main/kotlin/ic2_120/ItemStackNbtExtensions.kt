package ic2_120

import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

fun ItemStack.getCustomData(): NbtCompound? =
    this.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt()

fun ItemStack.getOrCreateCustomData(): NbtCompound {
    return this.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt() ?: NbtCompound()
}

inline fun ItemStack.editCustomData(editor: (NbtCompound) -> Unit) {
    val nbt = this.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt() ?: NbtCompound()
    editor(nbt)
    this.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
}

fun ItemStack.removeCustomData() {
    this.remove(DataComponentTypes.CUSTOM_DATA)
}

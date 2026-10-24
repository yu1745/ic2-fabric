package ic2_120

import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag

fun ItemStack.getCustomData(): CompoundTag? =
    this.get(DataComponents.CUSTOM_DATA)?.copyTag()

fun ItemStack.getOrCreateCustomData(): CompoundTag {
    return this.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
}

inline fun ItemStack.editCustomData(editor: (CompoundTag) -> Unit) {
    val nbt = this.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
    editor(nbt)
    this.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
}

fun ItemStack.removeCustomData() {
    this.remove(DataComponents.CUSTOM_DATA)
}

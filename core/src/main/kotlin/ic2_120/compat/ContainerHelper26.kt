package ic2_120.compat

import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.ProblemReporter
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput

object ContainerHelper26 {
    fun saveAllItems(nbt: CompoundTag, items: NonNullList<ItemStack>, lookup: HolderLookup.Provider) {
        val output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup)
        ContainerHelper.saveAllItems(output, items)
        nbt.merge(output.buildResult())
    }

    fun loadAllItems(nbt: CompoundTag, items: NonNullList<ItemStack>, lookup: HolderLookup.Provider) {
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, lookup, nbt), items)
    }
}

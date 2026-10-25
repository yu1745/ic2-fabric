package ic2_120.compat

import ic2_120.compat.addAfter

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ItemLike

fun FabricCreativeModeTabOutput.addAfter(anchor: ItemLike, stack: ItemStack) {
    val displayCount = displayStacks.size
    val searchCount = searchTabStacks.size
    accept(stack)
    if (displayStacks.size > displayCount) {
        val added = displayStacks.removeAt(displayStacks.lastIndex)
        val position = displayStacks.indexOfLast { it.item == anchor.asItem() }
        displayStacks.add(if (position >= 0) position + 1 else displayStacks.size, added)
    }
    if (searchTabStacks.size > searchCount) {
        val added = searchTabStacks.removeAt(searchTabStacks.lastIndex)
        val position = searchTabStacks.indexOfLast { it.item == anchor.asItem() }
        searchTabStacks.add(if (position >= 0) position + 1 else searchTabStacks.size, added)
    }
}

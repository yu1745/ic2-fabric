package ic2_120.content.uu

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

const val UU_TEMPLATE_NBT_KEY = "UuTemplate"
const val UU_TEMPLATE_LIST_NBT_KEY = "UuTemplates"

data class UuTemplateEntry(
    val itemId: String,
    val uuCostUb: Int
) {
    fun toNbt(): NbtCompound = NbtCompound().apply {
        putString("ItemId", itemId)
        putInt("UuCostUb", uuCostUb.coerceAtLeast(0))
    }

    fun displayName(): Text {
        val id = Identifier.tryParse(itemId)
        val item = id?.let { Registries.ITEM.get(it) }
        return if (item != null && item != net.minecraft.item.Items.AIR) {
            item.name.copy()
        } else {
            Text.literal(itemId)
        }
    }

    companion object {
        fun fromNbt(nbt: NbtCompound?): UuTemplateEntry? {
            if (nbt == null || nbt.isEmpty) return null
            val itemId = nbt.getString("ItemId")
            if (itemId.isBlank()) return null
            return UuTemplateEntry(itemId, nbt.getInt("UuCostUb").coerceAtLeast(0))
        }
    }
}

fun ItemStack.getUuTemplate(): UuTemplateEntry? =
    UuTemplateEntry.fromNbt(nbt?.getCompound(UU_TEMPLATE_NBT_KEY))

fun ItemStack.setUuTemplate(entry: UuTemplateEntry?) {
    val root = orCreateNbt
    if (entry == null) {
        root.remove(UU_TEMPLATE_NBT_KEY)
        if (root.isEmpty) nbt = null
        return
    }
    root.put(UU_TEMPLATE_NBT_KEY, entry.toNbt())
}

fun ItemStack.appendUuTemplateTooltip(tooltip: MutableList<Text>) {
    val template = getUuTemplate()
    if (template == null) {
        tooltip.add(Text.literal("<空>").formatted(Formatting.GRAY))
        return
    }
    tooltip.add(template.displayName().copy().formatted(Formatting.AQUA))
    tooltip.add(Text.literal("UU: ${template.uuCostUb} uB").formatted(Formatting.GRAY))
}

fun encodeUuTemplateList(entries: List<UuTemplateEntry>): NbtList =
    NbtList().apply { entries.forEach { add(it.toNbt()) } }

fun decodeUuTemplateList(list: NbtList?): MutableList<UuTemplateEntry> {
    if (list == null) return mutableListOf()
    val result = mutableListOf<UuTemplateEntry>()
    for (i in 0 until list.size) {
        UuTemplateEntry.fromNbt(list.getCompound(i))?.let(result::add)
    }
    return result
}

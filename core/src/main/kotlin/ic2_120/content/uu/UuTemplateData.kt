package ic2_120.content.uu

import ic2_120.config.Ic2Config
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation

const val UU_TEMPLATE_NBT_KEY = "UuTemplate"
const val UU_TEMPLATE_LIST_NBT_KEY = "UuTemplates"

data class UuTemplateEntry(
    val itemId: String
) {
    /** 服务端白名单中的 UU 费用，不在白名单则返回 0 */
    val uuCostUb: Int get() = Ic2Config.getReplicationCostUb(itemId) ?: 0

    fun toNbt(): CompoundTag = CompoundTag().apply {
        putString("ItemId", itemId)
    }

    fun displayName(): Component {
        val id = ResourceLocation.tryParse(itemId)
        val item = id?.let { BuiltInRegistries.ITEM.get(it) }
        return if (item != null && item != net.minecraft.world.item.Items.AIR) {
            item.name.copy()
        } else {
            Component.literal(itemId)
        }
    }

    companion object {
        fun fromNbt(nbt: CompoundTag?): UuTemplateEntry? {
            if (nbt == null || nbt.isEmpty) return null
            val itemId = nbt.getString("ItemId")
            if (itemId.isBlank()) return null
            return UuTemplateEntry(itemId)
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

fun ItemStack.appendUuTemplateTooltip(tooltip: MutableList<Component>) {
    val template = getUuTemplate()
    if (template == null) {
        tooltip.add(Component.literal("<空>").formatted(ChatFormatting.GRAY))
        return
    }
    tooltip.add(template.displayName().copy().formatted(ChatFormatting.AQUA))
    tooltip.add(Component.literal("UU: ${template.uuCostUb} uB").formatted(ChatFormatting.GRAY))
}

fun encodeUuTemplateList(entries: List<UuTemplateEntry>): ListTag =
    ListTag().apply { entries.forEach { add(it.toNbt()) } }

fun decodeUuTemplateList(list: ListTag?): MutableList<UuTemplateEntry> {
    if (list == null) return mutableListOf()
    val result = mutableListOf<UuTemplateEntry>()
    for (i in 0 until list.size) {
        UuTemplateEntry.fromNbt(list.getCompound(i))?.let(result::add)
    }
    return result
}

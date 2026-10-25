package ic2_120.content.uu

import ic2_120.compat.orCreateNbt
import ic2_120.getCustomData
import net.minecraft.core.component.DataComponents

import ic2_120.config.Ic2Config
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier

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
        val id = Identifier.tryParse(itemId)
        val item = id?.let { BuiltInRegistries.ITEM.getValue(it) }
        return if (item != null && item != net.minecraft.world.item.Items.AIR) {
            item.getName(net.minecraft.world.item.ItemStack(item)).copy()
        } else {
            Component.literal(itemId)
        }
    }

    companion object {
        fun fromNbt(nbt: CompoundTag?): UuTemplateEntry? {
            if (nbt == null || nbt.isEmpty) return null
            val itemId = nbt.getStringOr("ItemId", "")
            if (itemId.isBlank()) return null
            return UuTemplateEntry(itemId)
        }
    }
}

fun ItemStack.getUuTemplate(): UuTemplateEntry? =
    UuTemplateEntry.fromNbt(getCustomData()?.getCompoundOrEmpty(UU_TEMPLATE_NBT_KEY))

fun ItemStack.setUuTemplate(entry: UuTemplateEntry?) {
    // TODO(1.21.1): 组件时代 NBT 为复制语义，修改后必须通过 orCreateNbt 写回。
    val root = orCreateNbt
    if (entry == null) {
        root.remove(UU_TEMPLATE_NBT_KEY)
        if (root.isEmpty) {
            this.remove(DataComponents.CUSTOM_DATA)
        } else {
            orCreateNbt = root
        }
        return
    }
    root.put(UU_TEMPLATE_NBT_KEY, entry.toNbt())
    orCreateNbt = root
}

fun ItemStack.appendUuTemplateTooltip(tooltip: MutableList<Component>) {
    val template = getUuTemplate()
    if (template == null) {
        tooltip.add(Component.literal("<空>").withStyle(ChatFormatting.GRAY))
        return
    }
    tooltip.add(template.displayName().copy().withStyle(ChatFormatting.AQUA))
    tooltip.add(Component.literal("UU: ${template.uuCostUb} uB").withStyle(ChatFormatting.GRAY))
}

fun encodeUuTemplateList(entries: List<UuTemplateEntry>): ListTag =
    ListTag().apply { entries.forEach { add(it.toNbt()) } }

fun decodeUuTemplateList(list: ListTag?): MutableList<UuTemplateEntry> {
    if (list == null) return mutableListOf()
    val result = mutableListOf<UuTemplateEntry>()
    for (i in 0 until list.size) {
        UuTemplateEntry.fromNbt(list.getCompoundOrEmpty(i))?.let(result::add)
    }
    return result
}

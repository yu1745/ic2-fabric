package ic2_120.registry

import ic2_120.Ic2_120
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 将 [ModCreativeTab.iconResource]（贴图路径，如 `ic2:item/tool/electric/mining_laser`）解析为用于创造栏图标的 [ItemStack]。
 *
 * 不能使用同名玩法物品（如采矿镭射枪）作为图标，否则会在标签上绘制 EU/耐久等叠加层。
 * 因此每个用到的贴图应对应一个仅用于模型的占位物品（见 [ic2_120.content.item.TabIconIc2ToolsItem]，由 [ic2_120.content.item.CreativeTabIconItemsRegistration] 注册），
 * 并在 [TEXTURE_PATH_TO_ICON_ITEM] 中登记路径 → 占位物品 id。
 */
object CreativeTabIconProvider {

    /** 贴图路径（与注解中字符串一致）→ 仅图标的占位物品注册名 */
    private val TEXTURE_PATH_TO_ICON_ITEM: Map<String, String> = mapOf(
        "ic2:item/tool/electric/mining_laser" to "tab_icon_ic2_tools",
    )

    fun getIconStack(resourcePath: String): ItemStack {
        val key = resourcePath.trim()
        val iconName = TEXTURE_PATH_TO_ICON_ITEM[key]
        if (iconName != null) {
            val id = Identifier.of(Ic2_120.MOD_ID, iconName)
            val item = Registries.ITEM.get(id)
            if (item !== Items.AIR) return ItemStack(item)
        }
        return ItemStack(Items.BARRIER)
    }
}

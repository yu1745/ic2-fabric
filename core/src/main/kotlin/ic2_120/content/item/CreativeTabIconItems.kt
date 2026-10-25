package ic2_120.content.item

import net.minecraft.world.item.Item
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier

/**
 * 仅用于创造模式物品栏图标的占位物品：模型指向贴图，不参与玩法，无 EU/耐久条。
 * 不使用类扫描注解，由 [CreativeTabIconItemsRegistration] 在 [ic2_120.Ic2_120] 中手动注册。
 * 贴图路径映射见 [ic2_120.registry.CreativeTabIconProvider]。
 */
class TabIconIc2ToolsItem(id: Identifier) : Item(Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id)))

object CreativeTabIconItemsRegistration {
    fun register(modId: String) {
        val id = Identifier.fromNamespaceAndPath(modId, "tab_icon_ic2_tools")
        Registry.register(BuiltInRegistries.ITEM, id, TabIconIc2ToolsItem(id))
    }
}

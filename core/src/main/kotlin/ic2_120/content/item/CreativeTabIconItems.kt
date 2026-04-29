package ic2_120.content.item

import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * 仅用于创造模式物品栏图标的占位物品：模型指向贴图，不参与玩法，无 EU/耐久条。
 * 不使用类扫描注解，由 [CreativeTabIconItemsRegistration] 在 [ic2_120.Ic2_120] 中手动注册。
 * 贴图路径映射见 [ic2_120.registry.CreativeTabIconProvider]。
 */
class TabIconIc2ToolsItem : Item(FabricItemSettings())

object CreativeTabIconItemsRegistration {
    fun register(modId: String) {
        Registry.register(Registries.ITEM, Identifier(modId, "tab_icon_ic2_tools"), TabIconIc2ToolsItem())
    }
}

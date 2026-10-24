package ic2_120_advanced_weapons_addon.content.item

import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

class TabIconIc2AdvancedWeaponsItem : Item(Item.Settings())

object CreativeTabIconItemsRegistration {
    fun register(modId: String) {
        Registry.register(Registries.ITEM, Identifier(modId, "tab_icon_ic2_advanced_weapons"), TabIconIc2AdvancedWeaponsItem())
    }
}

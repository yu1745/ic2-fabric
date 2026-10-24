package ic2_120_advanced_weapons_addon.content.item

import net.minecraft.world.item.Item
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation

class TabIconIc2AdvancedWeaponsItem : Item(Item.Settings())

object CreativeTabIconItemsRegistration {
    fun register(modId: String) {
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation(modId, "tab_icon_ic2_advanced_weapons"), TabIconIc2AdvancedWeaponsItem())
    }
}

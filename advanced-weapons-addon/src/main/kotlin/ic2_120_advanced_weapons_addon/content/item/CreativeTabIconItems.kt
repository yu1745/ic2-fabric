package ic2_120_advanced_weapons_addon.content.item

import net.minecraft.world.item.Item
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

class TabIconIc2AdvancedWeaponsItem(properties: Item.Properties) : Item(properties)

object CreativeTabIconItemsRegistration {
    fun register(modId: String) {
        val id = Identifier.fromNamespaceAndPath(modId, "tab_icon_ic2_advanced_weapons")
        val key = ResourceKey.create(Registries.ITEM, id)
        Registry.register(BuiltInRegistries.ITEM, id, TabIconIc2AdvancedWeaponsItem(Item.Properties().setId(key)))
    }
}

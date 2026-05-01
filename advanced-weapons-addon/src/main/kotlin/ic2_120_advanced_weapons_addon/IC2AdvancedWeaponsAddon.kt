package ic2_120_advanced_weapons_addon

import ic2_120.registry.ClassScanner
import ic2_120_advanced_weapons_addon.content.item.CreativeTabIconItemsRegistration
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier

object IC2AdvancedWeaponsAddon : ModInitializer {
    const val MOD_ID = "ic2_120_advanced_weapons_addon"

    fun id(path: String): Identifier = Identifier.of(MOD_ID, path)

    override fun onInitialize() {
        CreativeTabIconItemsRegistration.register(MOD_ID)

        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120_advanced_weapons_addon.content.tab",
                "ic2_120_advanced_weapons_addon.content.item"
            )
        )
    }
}

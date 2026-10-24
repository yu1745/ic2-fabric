package ic2_120_advanced_weapons_addon.client

import ic2_120_advanced_weapons_addon.IC2AdvancedWeaponsAddon
import ic2_120_advanced_weapons_addon.content.item.QuantumSaber
import net.fabricmc.api.ClientModInitializer

import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

object IC2AdvancedWeaponsAddonClient : ClientModInitializer {
    private val QUANTUM_SABER_ACTIVE_ID = ResourceLocation.fromNamespaceAndPath(IC2AdvancedWeaponsAddon.MOD_ID, "quantum_saber_active")

    override fun onInitializeClient() {
        registerQuantumSaberPredicate()
    }

    private fun registerQuantumSaberPredicate() {
        val saber = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(IC2AdvancedWeaponsAddon.MOD_ID, "quantum_saber"))
        if (saber is QuantumSaber) {
            ItemProperties.register(
                saber,
                QUANTUM_SABER_ACTIVE_ID
            ) { stack, _, _, _ ->
                if (QuantumSaber.isActive(stack)) 1.0f else 0.0f
            }
        }
    }
}

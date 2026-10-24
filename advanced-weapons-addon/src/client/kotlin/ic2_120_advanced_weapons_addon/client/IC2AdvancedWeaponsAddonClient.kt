package ic2_120_advanced_weapons_addon.client

import ic2_120_advanced_weapons_addon.IC2AdvancedWeaponsAddon
import ic2_120_advanced_weapons_addon.content.item.QuantumSaber
import net.fabricmc.api.ClientModInitializer

import net.minecraft.client.item.ModelPredicateProviderRegistry
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object IC2AdvancedWeaponsAddonClient : ClientModInitializer {
<<<<<<< HEAD
    private val QUANTUM_SABER_ACTIVE_ID = Identifier(IC2AdvancedWeaponsAddon.MOD_ID, "quantum_saber_active")
=======
    private val QUANTUM_SABER_ACTIVE_ID = Identifier.of(IC2AdvancedWeaponsAddon.MOD_ID, "quantum_saber_active")
>>>>>>> backup/1.21.1

    override fun onInitializeClient() {
        registerQuantumSaberPredicate()
    }

    private fun registerQuantumSaberPredicate() {
<<<<<<< HEAD
        val saber = Registries.ITEM.get(Identifier(IC2AdvancedWeaponsAddon.MOD_ID, "quantum_saber"))
=======
        val saber = Registries.ITEM.get(Identifier.of(IC2AdvancedWeaponsAddon.MOD_ID, "quantum_saber"))
>>>>>>> backup/1.21.1
        if (saber is QuantumSaber) {
            ModelPredicateProviderRegistry.register(
                saber,
                QUANTUM_SABER_ACTIVE_ID
            ) { stack, _, _, _ ->
                if (QuantumSaber.isActive(stack)) 1.0f else 0.0f
            }
        }
    }
}

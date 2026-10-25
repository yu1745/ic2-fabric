package ic2_120_advanced_weapons_addon.client

import ic2_120_advanced_weapons_addon.IC2AdvancedWeaponsAddon
import ic2_120_advanced_weapons_addon.content.item.QuantumSaber
import net.fabricmc.api.ClientModInitializer
import com.mojang.serialization.MapCodec
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.item.ItemStack

object IC2AdvancedWeaponsAddonClient : ClientModInitializer {
    private val QUANTUM_SABER_ACTIVE_ID = Identifier.fromNamespaceAndPath(IC2AdvancedWeaponsAddon.MOD_ID, "quantum_saber_active")

    override fun onInitializeClient() {
        RangeSelectItemModelProperties.ID_MAPPER.put(QUANTUM_SABER_ACTIVE_ID, QuantumSaberActiveProperty.CODEC)
    }

    private object QuantumSaberActiveProperty : RangeSelectItemModelProperty {
        val CODEC: MapCodec<QuantumSaberActiveProperty> = MapCodec.unit(this)
        override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float =
            if (QuantumSaber.isActive(stack)) 1f else 0f
        override fun type(): MapCodec<QuantumSaberActiveProperty> = CODEC
    }
}

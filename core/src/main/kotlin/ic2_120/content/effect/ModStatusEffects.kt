package ic2_120.content.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

object ModStatusEffects {
    val SOLAR_GENERATING: MobEffect
        get() = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("ic2", "solar_generating"))!!

    val RADIATION: MobEffect
        get() = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("ic2", "radiation"))!!
}

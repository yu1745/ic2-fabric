package ic2_120.content.effect

import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object ModStatusEffects {
    val SOLAR_GENERATING: StatusEffect
        get() = Registries.STATUS_EFFECT.get(Identifier("ic2", "solar_generating"))!!

    val RADIATION: StatusEffect
        get() = Registries.STATUS_EFFECT.get(Identifier("ic2", "radiation"))!!
}

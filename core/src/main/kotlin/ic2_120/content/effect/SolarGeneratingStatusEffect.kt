package ic2_120.content.effect

import ic2_120.registry.annotation.ModStatusEffect
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory

@ModStatusEffect(name = "solar_generating", namespace = "ic2")
class SolarGeneratingStatusEffect : StatusEffect(StatusEffectCategory.BENEFICIAL, 0xF6D743)

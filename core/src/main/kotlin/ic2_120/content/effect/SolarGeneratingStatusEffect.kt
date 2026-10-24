package ic2_120.content.effect

import ic2_120.registry.annotation.ModStatusEffect
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

@ModStatusEffect(name = "solar_generating", namespace = "ic2")
class SolarGeneratingStatusEffect : MobEffect(MobEffectCategory.BENEFICIAL, 0xF6D743)

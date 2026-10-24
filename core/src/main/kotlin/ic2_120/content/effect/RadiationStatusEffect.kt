package ic2_120.content.effect

import net.minecraft.core.registries.Registries

import ic2_120.compat.world

import ic2_120.Ic2_120
import ic2_120.registry.annotation.ModStatusEffect
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.resources.ResourceLocation

@ModStatusEffect(name = "radiation", namespace = "ic2")
class RadiationStatusEffect : MobEffect(MobEffectCategory.HARMFUL, 0x55FF55) {

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return duration % 20 == 0
    }

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int): Boolean {
        val world = entity.level()
        if (world !is ServerLevel) return false

        val registry = world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
        val key = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "radiation")
        )
        val entry = registry.getHolder(key).orElse(null)
            ?: registry.getHolder(
                ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("minecraft", "magic"))
            ).orElse(null)

        return if (entry != null) {
            entity.hurt(DamageSource(entry), 1.0f)
            true
        } else {
            false
        }
    }
}

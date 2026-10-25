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
import net.minecraft.resources.Identifier

@ModStatusEffect(name = "radiation", namespace = "ic2")
class RadiationStatusEffect : MobEffect(MobEffectCategory.HARMFUL, 0x55FF55) {

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return duration % 20 == 0
    }

    override fun applyEffectTick(world: ServerLevel, entity: LivingEntity, amplifier: Int): Boolean {

        val registry = world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
        val key = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "radiation")
        )
        val entry = registry.get(key).orElse(null)
            ?: registry.get(
                ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("minecraft", "magic"))
            ).orElse(null)

        return if (entry != null) {
            entity.hurt(DamageSource(entry), 1.0f)
            true
        } else {
            false
        }
    }
}

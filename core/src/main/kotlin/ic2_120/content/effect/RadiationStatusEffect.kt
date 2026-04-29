package ic2_120.content.effect

import ic2_120.Ic2_120
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier

class RadiationStatusEffect : StatusEffect(StatusEffectCategory.HARMFUL, 0x55FF55) {

    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return duration % 20 == 0
    }

    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int) {
        val world = entity.world
        if (world !is ServerWorld) return

        val registry = world.registryManager.get(RegistryKeys.DAMAGE_TYPE)
        val key = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier(Ic2_120.MOD_ID, "radiation")
        )
        val entry = registry.getEntry(key).orElse(null)
            ?: registry.getEntry(
                RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier("minecraft", "magic"))
            ).orElse(null)

        if (entry != null) {
            entity.damage(DamageSource(entry), 1.0f)
        }
    }
}

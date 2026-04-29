package ic2_120.content.effect

import ic2_120.Ic2_120
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

import net.minecraft.util.Identifier

object ModStatusEffects {
    val SOLAR_GENERATING: StatusEffect = Registry.register(
        Registries.STATUS_EFFECT,
        Identifier("ic2", "solar_generating"),
        object : StatusEffect(StatusEffectCategory.BENEFICIAL, 0xF6D743) {}
    )

    val RADIATION: StatusEffect = Registry.register(
        Registries.STATUS_EFFECT,
        Identifier("ic2", "radiation"),
        RadiationStatusEffect()
    )

    fun register() {
        // 触发对象初始化，保证效果已注册。
    }
}


package ic2_120.registry.annotation

/**
 * 标记需要自动注册的 [net.minecraft.entity.effect.StatusEffect] 子类。
 *
 * 扫描器会通过无参构造创建实例并注册到 [net.minecraft.registry.Registries.STATUS_EFFECT]。
 *
 * @param name 注册名 path（不含命名空间）
 * @param namespace 命名空间；为空时使用 modId
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ModStatusEffect(
    val name: String,
    val namespace: String = ""
)

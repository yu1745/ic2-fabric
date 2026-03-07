package ic2_120.registry.annotation

/**
 * 标记需要自动注册的创造模式物品栏类。
 * 直接标注在类上，注册器会自动创建物品栏实例。
 *
 * 使用示例：
 * ```kotlin
 * @ModCreativeTab(name = "ic2_materials", iconItem = "copper_ingot")
 * class Ic2MaterialsTab
 * ```
 *
 * @param name 注册名（不含命名空间），如 "ic2_materials"
 * @param iconItem 图标物品的注册名（不含命名空间），物品必须已注册
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModCreativeTab(
    val name: String = "",
    val iconItem: String = ""
)

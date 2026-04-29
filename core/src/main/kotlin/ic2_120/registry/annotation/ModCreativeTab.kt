package ic2_120.registry.annotation

/**
 * 标记需要自动注册的创造模式物品栏类。
 * 直接标注在类上，注册器会自动创建物品栏实例。
 *
 * 使用示例：
 * ```kotlin
 * // 方式1：使用已注册的物品作为图标
 * @ModCreativeTab(name = "ic2_materials", iconItem = "copper_ingot")
 * class Ic2MaterialsTab
 *
 * // 方式2：直接指定贴图路径（映射到仅图标的占位物品，无耐久/EU 条；见 CreativeTabIconProvider）
 * @ModCreativeTab(name = "ic2_tools", iconResource = "ic2:item/tool/electric/mining_laser")
 * class Ic2ToolsTab
 * ```
 *
 * @param name 注册名（不含命名空间），如 "ic2_materials"
 * @param iconItem 图标物品的注册名（不含命名空间），物品必须已注册
 * @param iconResource 贴图路径（如 `ic2:item/...`），在 CreativeTabIconProvider 中映射到占位物品；优先于 iconItem
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModCreativeTab(
    val name: String = "",
    val iconItem: String = "",
    val iconResource: String = ""
)

package ic2_120.registry.annotation

import ic2_120.registry.CreativeTab

/**
 * 标记需要自动注册的方块类。
 * 直接标注在 Block 子类上，注册器会自动创建实例并注册。
 *
 * 使用示例：
 * ```kotlin
 * @ModBlock(name = "copper_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
 * class CopperBlock : Block(Settings.create().strength(5.0f))
 * ```
 *
 * @param name 注册名（不含命名空间），如 "copper_block"。为空则使用类名转换（驼峰转下划线小写）
 * @param registerItem 是否同时注册对应的方块物品（BlockItem）
 * @param tab 创造模式物品栏位置，使用枚举常量
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModBlock(
    val name: String = "",
    val registerItem: Boolean = true,
    val tab: CreativeTab = CreativeTab.MINECRAFT_MISC
)

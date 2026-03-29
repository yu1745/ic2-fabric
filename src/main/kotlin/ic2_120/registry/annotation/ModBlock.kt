package ic2_120.registry.annotation

import ic2_120.registry.CreativeTab
import ic2_120.registry.type

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
 * @param group 分组名；相同 group 的方块/物品在创造模式物品栏中会排在一起（反射顺序不稳定时用此保证顺序）。空字符串表示不分组
 * @param renderLayer 渲染层类型："cutout"、"cutout_mipped"、"translucent"；空字符串表示使用默认 solid 层。
 *                    translucency 渲染层与光影模组水下效果不兼容，如有水下渲染问题请使用 cutout/cutout_mipped
 * @param materialTags 语义路径（不含命名空间），如 `"ores/tin"`、`"storage_blocks/lead"`；用于 datagen 注册到 c:/forge:/ic2_120:compat/ 方块标签
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModBlock(
    val name: String = "",
    val registerItem: Boolean = true,
    val tab: CreativeTab = CreativeTab.MINECRAFT_MISC,
    val group: String = "",
    val renderLayer: String = "",
    val materialTags: Array<String> = emptyArray(),
)

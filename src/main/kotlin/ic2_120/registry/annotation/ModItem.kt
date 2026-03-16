package ic2_120.registry.annotation

import ic2_120.registry.CreativeTab
import ic2_120.registry.type

/**
 * 标记需要自动注册的物品类。
 * 直接标注在 Item 子类上，注册器会自动创建实例并注册。
 *
 * 使用示例：
 * ```kotlin
 * @ModItem(name = "copper_ingot", tab = CreativeTab.IC2_MATERIALS)
 * class CopperIngot : Item(FabricItemSettings())
 * ```
 *
 * @param name 注册名（不含命名空间），如 "copper_ingot"。为空则使用类名转换（驼峰转下划线小写）
 * @param tab 创造模式物品栏位置，使用枚举常量
 * @param group 分组名；相同 group 的物品在创造模式物品栏中会排在一起（反射顺序不稳定时用此保证顺序）。空字符串表示不分组
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModItem(
    val name: String = "",
    val tab: CreativeTab = CreativeTab.MINECRAFT_MISC,
    val group: String = ""
)

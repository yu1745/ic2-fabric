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
 * class CopperIngot : Item(Item.Settings())
 * ```
 *
 * @param name 注册名（不含命名空间），如 "copper_ingot"。为空则使用类名转换（驼峰转下划线小写）
 * @param tab 创造模式物品栏位置，使用枚举常量
 * @param group 分组名；相同 group 的物品在创造模式物品栏中会排在一起（反射顺序不稳定时用此保证顺序）。空字符串表示不分组
 * @param materialTags 语义路径（不含命名空间），如 `"ingots/tin"`、`"dusts/iron"`；用于 datagen 注册到 c:/forge:/ic2_120:compat/ 标签。
 *    适用于材料分类（矿物、锭、粉等），供其他 mod 通过标签查找物品。
 * @param tags 原版或第三方 item tag ID，如 `["minecraft:pickaxes", "minecraft:shovels"]`；用于 datagen 注册到指定命名空间的 tag。
 *    适用于工具类型标记（镐、铲、斧等），供原版机制或第三方 mod（如连锁采矿）识别。
 *    与 materialTags 的区别：materialTags 生成的是 c:/forge:/ic2_120 命名空间的工业材料分类标签；
 *    tags 直接注册到指定的 namespace:path（如 minecraft:pickaxes），影响原版行为。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModItem(
    val name: String = "",
    val tab: CreativeTab = CreativeTab.MINECRAFT_MISC,
    val group: String = "",
    val materialTags: Array<String> = emptyArray(),
    val tags: Array<String> = emptyArray(),
)

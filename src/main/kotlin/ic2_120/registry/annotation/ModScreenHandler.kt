package ic2_120.registry.annotation

import kotlin.reflect.KClass

/**
 * 标记需要自动注册的 ScreenHandler 类。
 * 直接标注在 ScreenHandler 子类上，注册器会自动创建 ExtendedScreenHandlerType 并注册。
 *
 * 约定：类需有 companion 对象，并提供静态方法
 * `fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): T`。
 *
 * 使用示例：
 * ```kotlin
 * // 方式一：指定 block，注册名与方块一致（推荐）
 * @ModScreenHandler(block = ElectricFurnaceBlock::class)
 * class ElectricFurnaceScreenHandler(...) : ScreenHandler(...)
 *
 * // 方式二：显式写 name
 * @ModScreenHandler(name = "electric_furnace")
 * class ElectricFurnaceScreenHandler(...) : ScreenHandler(...)
 * ```
 *
 * @param name 注册名（不含命名空间）。为空且未指定 block 时使用类名转换（驼峰转下划线小写）
 * @param block 对应的方块类；指定后注册名使用该方块的注册名，与 @ModBlock 保持一致
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModScreenHandler(
    val name: String = "",
    val block: KClass<*> = Any::class
)

package ic2_120.registry.annotation

import kotlin.reflect.KClass

/**
 * 标记需要自动注册的客户端 Screen 类。
 * 直接标注在 HandledScreen 子类上，客户端初始化时会自动注册到 HandledScreens。
 *
 * 使用示例：
 * ```kotlin
 * // 方式一：指定 block，handler 使用该方块的注册名（推荐）
 * @ModScreen(block = ElectricFurnaceBlock::class)
 * class ElectricFurnaceScreen(...) : HandledScreen<...>(...)
 *
 * // 方式二：显式写 handler 名
 * @ModScreen(handler = "electric_furnace")
 * class ElectricFurnaceScreen(...) : HandledScreen<...>(...)
 *
 * // 方式三：多个方块共用同一个 UI（使用 handlers 数组）
 * @ModScreen(handlers = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
 * class TransformerScreen(...) : HandledScreen<...>(...)
 * ```
 *
 * @param handler 对应的 ScreenHandler 注册名（与 @ModScreenHandler 的 name 一致）。未指定时若提供了 block 则用方块的注册名
 * @param handlers 多个 ScreenHandler 注册名（用于多个方块共用同一个 UI）
 * @param block 对应的方块类；指定后 handler 使用该方块的注册名，与 @ModBlock 保持一致
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModScreen(
    val handler: String = "",
    val handlers: Array<String> = [],
    val block: KClass<*> = Any::class
)

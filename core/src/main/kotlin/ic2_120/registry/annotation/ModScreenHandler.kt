package ic2_120.registry.annotation

import kotlin.reflect.KClass

/**
<<<<<<< HEAD
 * ScreenHandler 构造模式，决定扫描器如何从 [net.minecraft.network.PacketByteBuf] 构造客户端实例。
 *
 * - [BLOCK]：方块机器 UI，读取 `readBlockPos() + readVarInt()`。
 *   - [ModScreenHandler.inventorySize] > 0 → 构造签名 `(syncId, playerInventory, Inventory, ScreenHandlerContext, PropertyDelegate)`
 *   - [ModScreenHandler.inventorySize] == 0 → 构造签名 `(syncId, playerInventory, ScreenHandlerContext, PropertyDelegate)`
 * - [HANDHELD]：手持物品 UI，读取 `readEnumConstant(Hand::class.java)`，
 *   构造签名 `(syncId, playerInventory, Hand)`。
 */
enum class ScreenHandlerMode {
    BLOCK,
    HANDHELD,
}

/**
 * 标记需要自动注册的 ScreenHandler 类。
 * 直接标注在 ScreenHandler 子类上，注册器会自动创建 ExtendedScreenHandlerType 并注册。
 *
 * 约定：
 * - 标准模式（BLOCK / HANDHELD）：扫描器根据 [inventorySize] 和 [mode] 自动构造客户端实例，无需手写 fromBuffer。
 * - 自定义模式：类 companion 提供 `@ScreenFactory fun fromBuffer(syncId, playerInventory, buf): T`，
 *   扫描器优先使用它。用于 buf 协议或构造签名无法归入标准模式的特殊 ScreenHandler。
 *
 * 使用示例：
 * ```kotlin
 * // 方式一：指定 block + inventorySize（推荐，自动构造）
 * @ModScreenHandler(block = ElectricFurnaceBlock::class, inventorySize = ElectricFurnaceBlockEntity.INVENTORY_SIZE)
 * class ElectricFurnaceScreenHandler(...) : ScreenHandler(...)
 *
 * // 方式二：无 inventory 的方块机器
 * @ModScreenHandler(name = "kinetic_generator")
 * class KineticGeneratorScreenHandler(...) : ScreenHandler(...)
 *
 * // 方式三：手持物品 UI
 * @ModScreenHandler(name = "cropnalyzer", mode = ScreenHandlerMode.HANDHELD)
 * class CropnalyzerScreenHandler(...) : ScreenHandler(...)
 *
 * // 方式四：多个方块共用同一个 UI（使用 names 数组）
 * @ModScreenHandler(names = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
 * class TransformerScreenHandler(...) : ScreenHandler(...)
 *
 * // 方式五：自定义 fromBuffer（buf 协议特殊时）
 * @ModScreenHandler(block = MinerBlock::class)
 * class MinerScreenHandler(...) : ScreenHandler(...) {
 *     companion object {
 *         @ScreenFactory
 *         fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MinerScreenHandler { ... }
 *     }
 * }
=======
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
 *
 * // 方式三：多个方块共用同一个 UI（使用 names 数组）
 * @ModScreenHandler(names = ["lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer"])
 * class TransformerScreenHandler(...) : ScreenHandler(...)
>>>>>>> backup/1.21.1
 * ```
 *
 * @param name 注册名（不含命名空间）。为空且未指定 block 或 names 时使用类名转换（驼峰转下划线小写）
 * @param names 多个注册名（用于多个方块共用同一个 UI）
 * @param block 对应的方块类；指定后注册名使用该方块的注册名，与 @ModBlock 保持一致
<<<<<<< HEAD
 * @param inventorySize 客户端自动构造时使用的临时 Inventory 大小。
 *   引用 BE 常量如 `GeneratorBlockEntity.INVENTORY_SIZE`；`0` 表示无 inventory（4 参数构造）。
 * @param mode 构造模式，见 [ScreenHandlerMode]。默认 [ScreenHandlerMode.BLOCK]。
=======
 * @param clientInventorySize 当 companion 未提供 fromBuffer 时，
 * 客户端自动构造分支使用的临时 Inventory 大小。<= 0 表示禁用自动构造。
>>>>>>> backup/1.21.1
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModScreenHandler(
    val name: String = "",
    val names: Array<String> = [],
    val block: KClass<*> = Any::class,
<<<<<<< HEAD
    val inventorySize: Int = 0,
    val mode: ScreenHandlerMode = ScreenHandlerMode.BLOCK,
=======
    val clientInventorySize: Int = -1
>>>>>>> backup/1.21.1
)

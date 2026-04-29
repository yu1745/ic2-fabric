package ic2_120.registry.annotation

/**
 * 标记方块实体中作为 Fabric Transfer Item Storage 的属性。
 * 扫描注册时会对该 BlockEntityType 调用 [net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED].registerForBlockEntity，
 * 使物品管道等可通过 API Lookup 访问该存储。
 *
 * 使用示例：
 * ```kotlin
 * @ModBlockEntity(block = ElectricFurnaceBlock::class)
 * class ElectricFurnaceBlockEntity(...) : BlockEntity(...) {
 *     @RegisterItemStorage
 *     private val itemStorage = RoutedItemStorage(...)
 * }
 * ```
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisterItemStorage

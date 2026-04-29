package ic2_120.registry.annotation

/**
 * 标记方块实体中作为 Energy API 存储的字段/属性。
 * 扫描注册时会对该 BlockEntityType 调用 [team.reborn.energy.api.EnergyStorage.SIDED].registerForBlockEntity，
 * 使电缆等可通过 API Lookup 访问该存储。
 *
 * 使用示例：
 * ```kotlin
 * @ModBlockEntity(block = ElectricFurnaceBlock::class)
 * class ElectricFurnaceBlockEntity(...) : BlockEntity(...) {
 *     @RegisterEnergy
 *     val sync = ElectricFurnaceSync(syncedData)  // 继承 SimpleEnergyStorage
 * }
 * ```
 *
 * 可标在属性（property）或后备字段（@field:RegisterEnergy）上。
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisterEnergy

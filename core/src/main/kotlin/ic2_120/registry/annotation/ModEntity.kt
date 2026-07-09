package ic2_120.registry.annotation

/**
 * 标记需要自动注册的 [net.minecraft.entity.EntityType] 子类（实体类本身）。
 *
 * 扫描器会通过实体的 `(EntityType, World)` 构造函数创建 [EntityType.Builder.create] 工厂，
 * 然后注册到 [net.minecraft.registry.Registries.ENTITY_TYPE]。
 *
 * @param name 注册名 path（不含命名空间）
 * @param width 碰撞箱宽度
 * @param height 碰撞箱高度
 * @param spawnGroup 生成组名称（默认 "MISC"，对应 SpawnGroup 枚举名）
 * @param maxTrackingRange 最大追踪范围（格）
 * @param trackingTickInterval 同步间隔 tick（0 = 不设置，使用 MC 默认值 3）
 * @param dataFixerType 数据修复类型标识，如 `"minecraft:boat"` 或 `"minecraft:arrow"`
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ModEntity(
    val name: String,
    val width: Float,
    val height: Float,
    val spawnGroup: String = "MISC",
    val maxTrackingRange: Int = 10,
    val trackingTickInterval: Int = 0,
    val dataFixerType: String
)

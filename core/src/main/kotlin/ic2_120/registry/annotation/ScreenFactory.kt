package ic2_120.registry.annotation

/**
 * 标记 ScreenHandler companion 中用于构造客户端实例的工厂方法。
 *
 * 推荐签名：
 * `fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ScreenHandler`
 *
 * 说明：
 * - 该注解是可选的。
 * - 未标注时，扫描器仍会回退到约定方法名 `fromBuffer`。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScreenFactory


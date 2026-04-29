package ic2_120.registry.annotation

/**
 * 标记需要在 BlockEntityType 注册后自动执行的流体能力注册方法。
 *
 * 约定：
 * - 方法所在类需同时使用 @ModBlockEntity 参与自动注册；
 * - 方法应定义在 companion object 中；
 * - 方法签名应为无参函数（例如 `registerFluidStorageLookup()`）。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisterFluidStorage

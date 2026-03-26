package ic2_120.registry.annotation

/**
 * 标记 Block/Item companion 中的配方导出方法。
 *
 * 推荐签名：
 * `fun generateRecipes(exporter: Consumer<RecipeJsonProvider>)`
 *
 * 说明：
 * - 该注解是可选的。
 * - 未标注时，扫描器仍会回退到约定方法名 `generateRecipes`。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RecipeProvider


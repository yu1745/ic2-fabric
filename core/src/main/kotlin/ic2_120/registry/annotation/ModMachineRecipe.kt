package ic2_120.registry.annotation

import net.minecraft.recipe.Recipe
import kotlin.reflect.KClass

/**
 * 标注在 [net.minecraft.recipe.RecipeSerializer] 的 `object` 实现类上，声明注册 id 与配方类。
 * [ClassScanner] 会扫描 `ic2_120.content.recipes` 包；[ic2_120.content.recipes.ModMachineRecipes.register] 据此创建并注册 [net.minecraft.recipe.RecipeType] / [net.minecraft.recipe.RecipeSerializer]。
 *
 * 若需多个注册条目，请在各序列化器 `object` 上分别标注（例如金属成型机多种模式仍共用一个 [RecipeType]，只标一处序列化器即可）。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ModMachineRecipe(
    /** 注册路径（不含命名空间），须与 JSON 中 `type` 的 path 一致 */
    val id: String,
    /** 该序列化器对应的配方类 */
    val recipeClass: KClass<out Recipe<*>>,
)

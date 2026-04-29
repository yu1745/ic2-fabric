package ic2_120.registry.annotation

import net.minecraft.block.entity.BlockEntity
import net.minecraft.recipe.RecipeSerializer
import kotlin.reflect.KClass

/**
 * 标注在机器 [BlockEntity] 子类上，声明其使用的 [RecipeSerializer]（与 [ModMachineRecipe] 指向同一 object），
 * 以便 [ic2_120.content.recipes.ModMachineRecipes.getRecipeType] 通过 `this::class` 解析 [net.minecraft.recipe.RecipeType]。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ModMachineRecipeBinding(
    val serializerClass: KClass<out RecipeSerializer<*>>,
)

package ic2_120.registry

import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import kotlin.reflect.KClass

/** [ClassScanner.collectMachineRecipeRegistrations] 的扫描结果，供 [ic2_120.content.recipes.ModMachineRecipes.register] 使用 */
data class MachineRecipeScanEntry(
    val id: String,
    val recipeClass: KClass<out Recipe<*>>,
    val serializerClass: KClass<out RecipeSerializer<*>>,
)

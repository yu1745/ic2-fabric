package ic2_120_advanced_solar_addon.integration.jei

import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

@JeiPlugin
class Ic2AdvancedSolarAddonJeiPlugin : IModPlugin {
    companion object {
        @Volatile
        var jeiRuntime: IJeiRuntime? = null

        private var mtRecipes: List<MolecularTransformerJeiRecipe> = emptyList()

        fun refreshMTRecipes() {
            val runtime = jeiRuntime ?: return
            val oldRecipes = mtRecipes
            if (oldRecipes.isNotEmpty()) {
                runtime.recipeManager.hideRecipes(
                    Ic2AdvancedSolarAddonJeiRecipeTypes.MOLECULAR_TRANSFORMER,
                    oldRecipes
                )
            }
            val newRecipes = MTRecipes.getRecipes().map { recipe ->
                MolecularTransformerJeiRecipe(
                    input = recipe.input,
                    output = recipe.output,
                    energy = recipe.energy
                )
            }
            if (newRecipes.isNotEmpty()) {
                runtime.recipeManager.addRecipes(
                    Ic2AdvancedSolarAddonJeiRecipeTypes.MOLECULAR_TRANSFORMER,
                    newRecipes
                )
            }
            mtRecipes = newRecipes
        }
    }

    override fun getPluginUid(): Identifier {
        return Identifier("ic2_120_advanced_solar_addon", "jei")
    }

    override fun onRuntimeAvailable(runtime: IJeiRuntime) {
        Ic2AdvancedSolarAddonJeiPlugin.jeiRuntime = runtime
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(
            MolecularTransformerRecipeCategory(registration.jeiHelpers.guiHelper)
        )
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        mtRecipes = MTRecipes.getRecipes().map { recipe ->
            MolecularTransformerJeiRecipe(
                input = recipe.input,
                output = recipe.output,
                energy = recipe.energy
            )
        }
        registration.addRecipes(Ic2AdvancedSolarAddonJeiRecipeTypes.MOLECULAR_TRANSFORMER, mtRecipes)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120_advanced_solar_addon", "molecular_transformer"))),
            Ic2AdvancedSolarAddonJeiRecipeTypes.MOLECULAR_TRANSFORMER
        )
    }
}

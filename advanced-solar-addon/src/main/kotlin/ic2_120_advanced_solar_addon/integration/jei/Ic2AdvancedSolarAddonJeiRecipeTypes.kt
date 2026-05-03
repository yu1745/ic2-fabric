package ic2_120_advanced_solar_addon.integration.jei

import mezz.jei.api.recipe.RecipeType

object Ic2AdvancedSolarAddonJeiRecipeTypes {
    val MOLECULAR_TRANSFORMER: RecipeType<MolecularTransformerJeiRecipe> = RecipeType.create(
        "ic2_120_advanced_solar_addon",
        "molecular_transforming",
        MolecularTransformerJeiRecipe::class.java
    )
}

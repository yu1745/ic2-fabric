package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipe
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeSerializer
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

object ModCraftingRecipes {
    val BATTERY_ENERGY_SHAPED_TYPE: RecipeType<BatteryEnergyShapedRecipe> = object : RecipeType<BatteryEnergyShapedRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:battery_energy_shaped"
    }

    val BATTERY_ENERGY_SHAPED_SERIALIZER: RecipeSerializer<BatteryEnergyShapedRecipe> = BatteryEnergyShapedRecipeSerializer

    fun register() {
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("battery_energy_shaped"), BATTERY_ENERGY_SHAPED_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("battery_energy_shaped"), BATTERY_ENERGY_SHAPED_SERIALIZER)
    }
}

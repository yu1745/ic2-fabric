package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipe
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeSerializer
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipe
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeSerializer
import ic2_120.content.recipes.crafting.ConsumeWrenchShapedRecipe
import ic2_120.content.recipes.crafting.ConsumeWrenchShapedRecipeSerializer
import ic2_120.content.recipes.crafting.EmptyFluidCellToEmptyCellRecipe
import ic2_120.content.recipes.crafting.EmptyFluidCellToEmptyCellRecipeSerializer
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

object ModCraftingRecipes {
    val BATTERY_ENERGY_SHAPED_TYPE: RecipeType<BatteryEnergyShapedRecipe> = object : RecipeType<BatteryEnergyShapedRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:battery_energy_shaped"
    }

    val BATTERY_ENERGY_SHAPED_SERIALIZER: RecipeSerializer<BatteryEnergyShapedRecipe> = BatteryEnergyShapedRecipeSerializer

    val CONSUME_TREETAP_SHAPED_TYPE: RecipeType<ConsumeTreetapShapedRecipe> = object : RecipeType<ConsumeTreetapShapedRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:consume_treetap_shaped"
    }

    val CONSUME_TREETAP_SHAPED_SERIALIZER: RecipeSerializer<ConsumeTreetapShapedRecipe> = ConsumeTreetapShapedRecipeSerializer

    val CONSUME_WRENCH_SHAPED_TYPE: RecipeType<ConsumeWrenchShapedRecipe> = object : RecipeType<ConsumeWrenchShapedRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:consume_wrench_shaped"
    }

    val CONSUME_WRENCH_SHAPED_SERIALIZER: RecipeSerializer<ConsumeWrenchShapedRecipe> = ConsumeWrenchShapedRecipeSerializer

    val EMPTY_FLUID_CELL_TO_EMPTY_CELL_TYPE: RecipeType<EmptyFluidCellToEmptyCellRecipe> =
        object : RecipeType<EmptyFluidCellToEmptyCellRecipe> {
            override fun toString(): String = "${Ic2_120.MOD_ID}:empty_fluid_cell_to_empty_cell"
        }

    val EMPTY_FLUID_CELL_TO_EMPTY_CELL_SERIALIZER: RecipeSerializer<EmptyFluidCellToEmptyCellRecipe> =
        EmptyFluidCellToEmptyCellRecipeSerializer

    fun register() {
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("battery_energy_shaped"), BATTERY_ENERGY_SHAPED_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("battery_energy_shaped"), BATTERY_ENERGY_SHAPED_SERIALIZER)
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("consume_treetap_shaped"), CONSUME_TREETAP_SHAPED_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("consume_treetap_shaped"), CONSUME_TREETAP_SHAPED_SERIALIZER)
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("consume_wrench_shaped"), CONSUME_WRENCH_SHAPED_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("consume_wrench_shaped"), CONSUME_WRENCH_SHAPED_SERIALIZER)
        Registry.register(
            Registries.RECIPE_TYPE,
            Ic2_120.id("empty_fluid_cell_to_empty_cell"),
            EMPTY_FLUID_CELL_TO_EMPTY_CELL_TYPE
        )
        Registry.register(
            Registries.RECIPE_SERIALIZER,
            Ic2_120.id("empty_fluid_cell_to_empty_cell"),
            EMPTY_FLUID_CELL_TO_EMPTY_CELL_SERIALIZER
        )
    }
}

package ic2_120_advanced_solar_addon.content.recipe

import ic2_120_advanced_solar_addon.config.Ic2AdvancedSolarAddonConfig
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import net.minecraft.registry.Registries

object MTRecipes {
    private val recipes = mutableListOf<MTRecipe>()

    data class MTRecipe(
        val input: ItemStack,
        val output: ItemStack,
        val energy: Long
    )

    fun init() {
        loadFromConfig()
    }

    fun loadFromConfig() {
        recipes.clear()
        for (configRecipe in Ic2AdvancedSolarAddonConfig.getMolecularTransformerRecipes()) {
            addRecipe(configRecipe.input, configRecipe.output, configRecipe.energy)
        }
    }

    fun loadFromSync(entries: List<MTRecipeEntry>) {
        recipes.clear()
        for (entry in entries) {
            addRecipe(entry.inputId, entry.outputId, entry.energy)
        }
    }

    private fun addRecipe(inputId: String, outputId: String, energy: Long) {
        val inId = Identifier.tryParse(inputId)
        val outId = Identifier.tryParse(outputId)
        if (inId != null && outId != null) {
            val inputItem = Registries.ITEM.get(inId)
            val outputItem = Registries.ITEM.get(outId)
            if (inputItem != Items.AIR && outputItem != Items.AIR && energy > 0) {
                recipes.add(MTRecipe(
                    input = ItemStack(inputItem),
                    output = ItemStack(outputItem),
                    energy = energy
                ))
            }
        }
    }

    fun findRecipe(input: ItemStack): MTRecipe? {
        return recipes.find { recipe ->
            ItemStack.areItemsAndComponentsEqual(recipe.input, input) && input.count >= recipe.input.count
        }
    }

    fun getRecipes(): List<MTRecipe> = recipes.toList()
}

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
        // 从配置文件加载配方
        val configRecipes = Ic2AdvancedSolarAddonConfig.getMolecularTransformerRecipes()
        recipes.clear()

        for (configRecipe in configRecipes) {
            val inputId = Identifier.tryParse(configRecipe.input)
            val outputId = Identifier.tryParse(configRecipe.output)

            if (inputId != null && outputId != null) {
                val inputItem = Registries.ITEM.get(inputId)
                val outputItem = Registries.ITEM.get(outputId)

                if (inputItem != Items.AIR && outputItem != Items.AIR && configRecipe.energy > 0) {
                    recipes.add(MTRecipe(
                        input = ItemStack(inputItem),
                        output = ItemStack(outputItem),
                        energy = configRecipe.energy
                    ))
                }
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

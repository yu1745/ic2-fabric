package ic2_120.content.recipes.blockcutter

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.core.HolderLookup
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

class BlockCutterRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val inputCount: Int,
    val materialHardness: Float,
    val output: ItemStack
) : Recipe<RecipeInput> {
    init {
        require(inputCount == 1 || inputCount == 2) { "BlockCutter inputCount must be 1 or 2" }
    }

    override fun matches(input: RecipeInput, world: Level): Boolean {
        val stack = input.getStackInSlot(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(input: RecipeInput, lookup: HolderLookup.WrapperLookup): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getResult(lookup: HolderLookup.WrapperLookup): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(BlockCutterRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(BlockCutterRecipe::class)

    fun isBladeSufficient(bladeHardness: Float): Boolean {
        return bladeHardness > materialHardness
    }
}

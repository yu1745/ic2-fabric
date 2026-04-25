package ic2_120.content.recipes.crafting

import com.mojang.serialization.MapCodec
import ic2_120.content.item.isFluidCellEmpty
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World
import java.util.stream.Stream

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())

/**
 * 工作台无序配方：仅允许空的 fluid_cell 转换为 empty_cell。
 * 需要覆写余物逻辑，避免 fluid_cell 自带余物再次返还 empty_cell。
 */
class EmptyFluidCellToEmptyCellRecipe(delegate: ShapelessRecipe) : ShapelessRecipe(
    delegate.group,
    delegate.category,
    delegate.getResult(EMPTY_LOOKUP),
    delegate.ingredients
) {
    private val fluidCellItem = delegate.ingredients.firstOrNull()?.matchingStacks?.firstOrNull()?.item

    override fun matches(input: CraftingRecipeInput, world: World): Boolean {
        if (!super.matches(input, world)) return false

        var found = false
        for (slot in 0 until input.getSize()) {
            val stack = input.getStackInSlot(slot)
            if (stack.isEmpty) continue
            if (stack.item != fluidCellItem) return false
            if (!stack.isFluidCellEmpty()) return false
            if (found) return false
            found = true
        }
        return found
    }

    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> =
        DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)

    override fun getSerializer(): RecipeSerializer<*> = EmptyFluidCellToEmptyCellRecipeSerializer
}

object EmptyFluidCellToEmptyCellRecipeSerializer : RecipeSerializer<EmptyFluidCellToEmptyCellRecipe> {
    override fun codec(): MapCodec<EmptyFluidCellToEmptyCellRecipe> =
        RecipeSerializer.SHAPELESS.codec().xmap(
            { EmptyFluidCellToEmptyCellRecipe(it) },
            { it }
        )

    override fun packetCodec(): PacketCodec<RegistryByteBuf, EmptyFluidCellToEmptyCellRecipe> =
        RecipeSerializer.SHAPELESS.packetCodec().xmap(
            { EmptyFluidCellToEmptyCellRecipe(it) },
            { it }
        )
}

object EmptyFluidCellToEmptyCellRecipeDatagen {
    fun offer(
        exporter: RecipeExporter,
        recipeId: Identifier,
        input: Item,
        result: Item,
        count: Int = 1,
        category: String = "misc"
    ) {
        val ingredients = DefaultedList.ofSize<Ingredient>(1, Ingredient.EMPTY)
        ingredients[0] = Ingredient.ofItems(input)
        val shapeless = ShapelessRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), ItemStack(result, count), ingredients)
        val recipe = EmptyFluidCellToEmptyCellRecipe(shapeless)
        exporter.accept(recipeId, recipe, null)
    }
}

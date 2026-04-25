package ic2_120.content.recipes.crafting

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.item.isFluidCellEmpty
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World
import java.util.function.Consumer

/**
 * 工作台无序配方：仅允许空的 fluid_cell 转换为 empty_cell。
 * 需要覆写余物逻辑，避免 fluid_cell 自带余物再次返还 empty_cell。
 */
class EmptyFluidCellToEmptyCellRecipe(delegate: ShapelessRecipe) : ShapelessRecipe(
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.getResult(RegistryWrapper.WrapperLookup.EMPTY),
    delegate.ingredients
) {
    private val fluidCellItem = delegate.ingredients.firstOrNull()?.matchingStacks?.firstOrNull()?.item

    override fun matches(inventory: RecipeInputInventory, world: World): Boolean {
        if (!super.matches(inventory, world)) return false

        var found = false
        for (slot in 0 until inventory.size()) {
            val stack = inventory.getStack(slot)
            if (stack.isEmpty) continue
            if (stack.item != fluidCellItem) return false
            if (!stack.isFluidCellEmpty()) return false
            if (found) return false
            found = true
        }
        return found
    }

    override fun getRemainder(inventory: RecipeInputInventory): DefaultedList<ItemStack> =
        DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY)

    override fun getSerializer(): RecipeSerializer<*> = EmptyFluidCellToEmptyCellRecipeSerializer
}

object EmptyFluidCellToEmptyCellRecipeSerializer : RecipeSerializer<EmptyFluidCellToEmptyCellRecipe> {
    override fun read(id: Identifier, json: JsonObject): EmptyFluidCellToEmptyCellRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, json)
        return EmptyFluidCellToEmptyCellRecipe(shapeless)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): EmptyFluidCellToEmptyCellRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, buf)
        return EmptyFluidCellToEmptyCellRecipe(shapeless)
    }

    override fun write(buf: PacketByteBuf, recipe: EmptyFluidCellToEmptyCellRecipe) {
        RecipeSerializer.SHAPELESS.write(buf, recipe)
    }
}

object EmptyFluidCellToEmptyCellRecipeDatagen {
    fun offer(
        exporter: Consumer<RecipeExporter>,
        recipeId: Identifier,
        input: Item,
        result: Item,
        count: Int = 1,
        category: String = "misc"
    ) {
        exporter.accept(
            Provider(
                recipeId = recipeId,
                input = input,
                result = result,
                count = count,
                category = category
            )
        )
    }

    private class Provider(
        private val recipeId: Identifier,
        private val input: Item,
        private val result: Item,
        private val count: Int,
        private val category: String
    ) : RecipeExporter {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "ic2_120:empty_fluid_cell_to_empty_cell")
            json.addProperty("category", category)

            val ingredients = JsonArray()
            val ing = JsonObject()
            ing.addProperty("item", Registries.ITEM.getId(input).toString())
            ingredients.add(ing)
            json.add("ingredients", ingredients)

            val resultObj = JsonObject()
            resultObj.addProperty("item", Registries.ITEM.getId(result).toString())
            if (count > 1) resultObj.addProperty("count", count)
            json.add("result", resultObj)
        }

        override fun getSerializer(): RecipeSerializer<*> = EmptyFluidCellToEmptyCellRecipeSerializer

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}

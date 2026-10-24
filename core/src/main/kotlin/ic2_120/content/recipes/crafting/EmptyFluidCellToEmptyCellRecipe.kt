package ic2_120.content.recipes.crafting

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import net.minecraft.world.level.Level
import java.util.function.Consumer

/**
 * 工作台无序配方：仅允许空的 fluid_cell 转换为 empty_cell。
 * 需要覆写余物逻辑，避免 fluid_cell 自带余物再次返还 empty_cell。
 */
class EmptyFluidCellToEmptyCellRecipe(delegate: ShapelessRecipe) : ShapelessRecipe(
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.getOutput(RegistryAccess.EMPTY),
    delegate.ingredients
) {
    private val fluidCellItem = delegate.ingredients.firstOrNull()?.matchingStacks?.firstOrNull()?.item

    override fun matches(inventory: CraftingContainer, world: Level): Boolean {
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

    override fun getRemainder(inventory: CraftingContainer): NonNullList<ItemStack> =
        NonNullList.ofSize(inventory.size(), ItemStack.EMPTY)

    override fun getSerializer(): RecipeSerializer<*> = EmptyFluidCellToEmptyCellRecipeSerializer
}

@ModMachineRecipe(id = "empty_fluid_cell_to_empty_cell", recipeClass = EmptyFluidCellToEmptyCellRecipe::class)
object EmptyFluidCellToEmptyCellRecipeSerializer : RecipeSerializer<EmptyFluidCellToEmptyCellRecipe> {
    override fun read(id: ResourceLocation, json: JsonObject): EmptyFluidCellToEmptyCellRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, json)
        return EmptyFluidCellToEmptyCellRecipe(shapeless)
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): EmptyFluidCellToEmptyCellRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, buf)
        return EmptyFluidCellToEmptyCellRecipe(shapeless)
    }

    override fun write(buf: FriendlyByteBuf, recipe: EmptyFluidCellToEmptyCellRecipe) {
        RecipeSerializer.SHAPELESS.write(buf, recipe)
    }
}

object EmptyFluidCellToEmptyCellRecipeDatagen {
    fun offer(
        exporter: Consumer<RecipeOutput>,
        recipeId: ResourceLocation,
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
        private val recipeId: ResourceLocation,
        private val input: Item,
        private val result: Item,
        private val count: Int,
        private val category: String
    ) : RecipeOutput {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "ic2_120:empty_fluid_cell_to_empty_cell")
            json.addProperty("category", category)

            val ingredients = JsonArray()
            val ing = JsonObject()
            ing.addProperty("item", BuiltInRegistries.ITEM.getId(input).toString())
            ingredients.add(ing)
            json.add("ingredients", ingredients)

            val resultObj = JsonObject()
            resultObj.addProperty("item", BuiltInRegistries.ITEM.getId(result).toString())
            if (count > 1) resultObj.addProperty("count", count)
            json.add("result", resultObj)
        }

        override fun getSerializer(): RecipeSerializer<*> = EmptyFluidCellToEmptyCellRecipeSerializer

        override fun getRecipeId(): ResourceLocation = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): ResourceLocation? = null
    }
}

package ic2_120.content.recipes.crafting

<<<<<<< HEAD
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.registry.DynamicRegistryManager
=======
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
>>>>>>> backup/1.21.1
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World
<<<<<<< HEAD
import java.util.function.Consumer
=======
import java.util.stream.Stream

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())
>>>>>>> backup/1.21.1

/**
 * 工作台无序配方：仅允许空的 fluid_cell 转换为 empty_cell。
 * 需要覆写余物逻辑，避免 fluid_cell 自带余物再次返还 empty_cell。
 */
class EmptyFluidCellToEmptyCellRecipe(delegate: ShapelessRecipe) : ShapelessRecipe(
<<<<<<< HEAD
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.getOutput(DynamicRegistryManager.EMPTY),
=======
    delegate.group,
    delegate.category,
    delegate.getResult(EMPTY_LOOKUP),
>>>>>>> backup/1.21.1
    delegate.ingredients
) {
    private val fluidCellItem = delegate.ingredients.firstOrNull()?.matchingStacks?.firstOrNull()?.item

<<<<<<< HEAD
    override fun matches(inventory: RecipeInputInventory, world: World): Boolean {
        if (!super.matches(inventory, world)) return false

        var found = false
        for (slot in 0 until inventory.size()) {
            val stack = inventory.getStack(slot)
=======
    override fun matches(input: CraftingRecipeInput, world: World): Boolean {
        if (!super.matches(input, world)) return false

        var found = false
        for (slot in 0 until input.getSize()) {
            val stack = input.getStackInSlot(slot)
>>>>>>> backup/1.21.1
            if (stack.isEmpty) continue
            if (stack.item != fluidCellItem) return false
            if (!stack.isFluidCellEmpty()) return false
            if (found) return false
            found = true
        }
        return found
    }

<<<<<<< HEAD
    override fun getRemainder(inventory: RecipeInputInventory): DefaultedList<ItemStack> =
        DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY)
=======
    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> =
        DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)
>>>>>>> backup/1.21.1

    override fun getSerializer(): RecipeSerializer<*> = EmptyFluidCellToEmptyCellRecipeSerializer
}

<<<<<<< HEAD
@ModMachineRecipe(id = "empty_fluid_cell_to_empty_cell", recipeClass = EmptyFluidCellToEmptyCellRecipe::class)
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
=======
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
>>>>>>> backup/1.21.1
}

object EmptyFluidCellToEmptyCellRecipeDatagen {
    fun offer(
<<<<<<< HEAD
        exporter: Consumer<RecipeJsonProvider>,
=======
        exporter: RecipeExporter,
>>>>>>> backup/1.21.1
        recipeId: Identifier,
        input: Item,
        result: Item,
        count: Int = 1,
        category: String = "misc"
    ) {
<<<<<<< HEAD
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
    ) : RecipeJsonProvider {
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
=======
        val ingredients = DefaultedList.ofSize<Ingredient>(1, Ingredient.EMPTY)
        ingredients[0] = Ingredient.ofItems(input)
        val shapeless = ShapelessRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), ItemStack(result, count), ingredients)
        val recipe = EmptyFluidCellToEmptyCellRecipe(shapeless)
        exporter.accept(recipeId, recipe, null)
>>>>>>> backup/1.21.1
    }
}

package ic2_120.content.recipes.crafting

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.serialization.MapCodec
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.recipes.INGREDIENT_CODEC
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.level.Level

/**
 * 工作台无序配方：仅允许空的 fluid_cell 转换为 empty_cell。
 * 需要覆写余物逻辑，避免 fluid_cell 自带余物再次返还 empty_cell。
 */
class EmptyFluidCellToEmptyCellRecipe(
    internal val recipeIngredients: List<Ingredient>,
    result: ItemStack,
    group: String = "",
) : ShapelessRecipe(group, CraftingBookCategory.MISC, result, NonNullList.of(Ingredient.EMPTY, *recipeIngredients.toTypedArray())) {
    private val fluidCellItem: Item? =
        recipeIngredients.firstOrNull()?.items?.firstOrNull()?.item

    override fun matches(inventory: CraftingInput, world: Level): Boolean {
        if (!super.matches(inventory, world)) return false

        var found = false
        for (slot in 0 until inventory.size()) {
            val stack = inventory.getItem(slot)
            if (stack.isEmpty) continue
            if (stack.item != fluidCellItem) return false
            if (!stack.isFluidCellEmpty()) return false
            if (found) return false
            found = true
        }
        return found
    }

    override fun getRemainingItems(inventory: CraftingInput): NonNullList<ItemStack> =
        NonNullList.withSize(inventory.size(), ItemStack.EMPTY)

    override fun getSerializer(): RecipeSerializer<*> = EmptyFluidCellToEmptyCellRecipeSerializer
}

fun decodeEmptyFluidCellToEmptyCellJson(id: ResourceLocation, json: JsonObject): EmptyFluidCellToEmptyCellRecipe {
    val ingredients = json.getAsJsonArray("ingredients").map { INGREDIENT_CODEC(it.asJsonObject) }
    val resultObj = json.getAsJsonObject("result")
    val resultItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resultObj.get("item").asString))
    val count = if (resultObj.has("count")) resultObj.get("count").asInt else 1
    return EmptyFluidCellToEmptyCellRecipe(ingredients, ItemStack(resultItem, count))
}

fun encodeEmptyFluidCellToEmptyCellJson(json: JsonObject, recipe: EmptyFluidCellToEmptyCellRecipe) {
    json.addProperty("type", "ic2_120:empty_fluid_cell_to_empty_cell")

    val ingredients = JsonArray()
    for (ingredient in recipe.getIngredients()) {
        val obj = JsonObject()
        val first = ingredient.items.firstOrNull()
        obj.addProperty("item", first?.let { BuiltInRegistries.ITEM.getKey(it.item)?.toString() } ?: "minecraft:air")
        ingredients.add(obj)
    }
    json.add("ingredients", ingredients)

    val result = recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY)
    val resultObj = JsonObject()
    BuiltInRegistries.ITEM.getKey(result.item)?.let { resultObj.addProperty("item", it.toString()) }
    if (result.count > 1) resultObj.addProperty("count", result.count)
    json.add("result", resultObj)
}

@ModMachineRecipe(id = "empty_fluid_cell_to_empty_cell", recipeClass = EmptyFluidCellToEmptyCellRecipe::class)
object EmptyFluidCellToEmptyCellRecipeSerializer : RecipeSerializer<EmptyFluidCellToEmptyCellRecipe> {
    override fun codec(): MapCodec<EmptyFluidCellToEmptyCellRecipe> = ic2_120.content.recipes.RecipeCodecBridge.jsonMapCodec(
        "ic2_120:empty_fluid_cell_to_empty_cell",
        ::decodeEmptyFluidCellToEmptyCellJson,
        ::encodeEmptyFluidCellToEmptyCellJson
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, EmptyFluidCellToEmptyCellRecipe> {
        return StreamCodec.ofMember(
            { recipe: EmptyFluidCellToEmptyCellRecipe, buf: RegistryFriendlyByteBuf ->
                buf.writeUtf(recipe.group)
                buf.writeVarInt(recipe.recipeIngredients.size)
                recipe.recipeIngredients.forEach { Ingredient.CONTENTS_STREAM_CODEC.encode(buf, it) }
                ItemStack.STREAM_CODEC.encode(buf, recipe.getResultItem(buf.registryAccess()))
            },
            { buf: RegistryFriendlyByteBuf ->
                val group = buf.readUtf()
                val ingredients = List(buf.readVarInt()) { Ingredient.CONTENTS_STREAM_CODEC.decode(buf) }
                EmptyFluidCellToEmptyCellRecipe(ingredients, ItemStack.STREAM_CODEC.decode(buf), group)
            }
        )
    }
}

object EmptyFluidCellToEmptyCellRecipeDatagen {
    fun offer(
        exporter: net.minecraft.data.recipes.RecipeOutput,
        recipeId: ResourceLocation,
        input: Item,
        result: Item,
        count: Int = 1,
        category: String = "misc"
    ) {
        val recipe = EmptyFluidCellToEmptyCellRecipe(listOf(Ingredient.of(input)), ItemStack(result, count))
        exporter.accept(recipeId, recipe, null)
    }
}

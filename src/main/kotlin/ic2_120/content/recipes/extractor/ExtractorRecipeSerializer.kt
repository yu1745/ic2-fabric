package ic2_120.content.recipes.extractor

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack

import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

@ModMachineRecipe(id = "extracting", recipeClass = ExtractorRecipe::class)
object ExtractorRecipeSerializer : RecipeSerializer<ExtractorRecipe> {
    override fun read(id: Identifier, json: JsonObject): ExtractorRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        return ExtractorRecipe(id, ingredient, ItemStack(item, count))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): ExtractorRecipe {
        val ingredient = Ingredient.PACKET_CODEC.decode(buf)
        val output = ItemStack.PACKET_CODEC.decode(buf)
        return ExtractorRecipe(id, ingredient, output)
    }

    override fun write(buf: PacketByteBuf, recipe: ExtractorRecipe) {
        Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
        ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
    }
}

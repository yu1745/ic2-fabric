package ic2_120.content.recipes.compressor

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack

import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

@ModMachineRecipe(id = "compressing", recipeClass = CompressorRecipe::class)
object CompressorRecipeSerializer : RecipeSerializer<CompressorRecipe> {
    override fun read(id: Identifier, json: JsonObject): CompressorRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))
        val inputCount = JsonHelper.getInt(json, "input_count", 1)
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        return CompressorRecipe(id, ingredient, inputCount, ItemStack(item, count))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): CompressorRecipe {
        val ingredient = Ingredient.PACKET_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        val output = ItemStack.PACKET_CODEC.decode(buf)
        return CompressorRecipe(id, ingredient, inputCount, output)
    }

    override fun write(buf: PacketByteBuf, recipe: CompressorRecipe) {
        Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
        buf.writeVarInt(recipe.inputCount)
        ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
    }
}

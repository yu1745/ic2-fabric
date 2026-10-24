package ic2_120.content.recipes.compressor

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "compressing", recipeClass = CompressorRecipe::class)
object CompressorRecipeSerializer : RecipeSerializer<CompressorRecipe> {
    override fun read(id: ResourceLocation, json: JsonObject): CompressorRecipe {
        val ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "ingredient"))
        val inputCount = GsonHelper.getInt(json, "input_count", 1)
        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val item = BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)
        val containerReturn = if (json.has("container_return")) {
            val containerObj = json.getAsJsonObject("container_return")
            val containerId = ResourceLocation.parse(GsonHelper.getString(containerObj, "item"))
            val containerCount = GsonHelper.getInt(containerObj, "count", 1)
            ItemStack(BuiltInRegistries.ITEM.get(containerId), containerCount)
        } else ItemStack.EMPTY
        return CompressorRecipe(id, ingredient, inputCount, ItemStack(item, count), containerReturn)
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): CompressorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val inputCount = buf.readVarInt()
        val output = buf.readItemStack()
        val containerReturn = buf.readItemStack()
        return CompressorRecipe(id, ingredient, inputCount, output, containerReturn)
    }

    override fun write(buf: FriendlyByteBuf, recipe: CompressorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeVarInt(recipe.inputCount)
        buf.writeItemStack(recipe.output.copy())
        buf.writeItemStack(recipe.containerReturn.copy())
    }
}

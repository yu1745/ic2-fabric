package ic2_120.content.recipes.extractor

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "extracting", recipeClass = ExtractorRecipe::class)
object ExtractorRecipeSerializer : RecipeSerializer<ExtractorRecipe> {
    override fun read(id: ResourceLocation, json: JsonObject): ExtractorRecipe {
        val ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "ingredient"))
        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val item = BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)
        return ExtractorRecipe(id, ingredient, ItemStack(item, count))
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): ExtractorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        return ExtractorRecipe(id, ingredient, output)
    }

    override fun write(buf: FriendlyByteBuf, recipe: ExtractorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())
    }
}

package ic2_120.content.recipes.macerator

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "macerating", recipeClass = MaceratorRecipe::class)
object MaceratorRecipeSerializer : RecipeSerializer<MaceratorRecipe> {
    override fun read(id: ResourceLocation, json: JsonObject): MaceratorRecipe {
        val ingredientJson = GsonHelper.getObject(json, "ingredient")
        val count = GsonHelper.getInt(ingredientJson, "count", 1)
        val ingredient = Ingredient.fromJson(ingredientJson)

        val result = GsonHelper.getObject(json, "result")
        val resultItemId = ResourceLocation(GsonHelper.getString(result, "item"))
        val resultItem = BuiltInRegistries.ITEM.get(resultItemId)
        val resultCount = GsonHelper.getInt(result, "count", 1)
        return MaceratorRecipe(id, ingredient, ItemStack(resultItem, resultCount), count)
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): MaceratorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        val inputCount = buf.readVarInt()
        return MaceratorRecipe(id, ingredient, output, inputCount)
    }

    override fun write(buf: FriendlyByteBuf, recipe: MaceratorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())
        buf.writeVarInt(recipe.inputCount)
    }
}

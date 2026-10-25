package ic2_120.content.recipes.macerator

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.content.recipes.INGREDIENT_CODEC
import ic2_120.content.recipes.RecipeCodecBridge

import net.minecraft.network.codec.StreamCodec

import net.minecraft.network.RegistryFriendlyByteBuf

import com.mojang.serialization.MapCodec

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.resources.Identifier
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "macerating", recipeClass = MaceratorRecipe::class)
object MaceratorRecipeSerializer : ic2_120.content.recipes.RecipeSerializerHolder26<MaceratorRecipe>() {
    private fun fromJson(id: Identifier, json: JsonObject): MaceratorRecipe {
        val ingredientJson = json.getAsJsonObject("ingredient")
        val count = (if (ingredientJson.has("count")) ingredientJson.get("count").asInt else 1)
        val ingredient = INGREDIENT_CODEC(ingredientJson)

        val result = json.getAsJsonObject("result")
        val resultItemId = Identifier.parse(result.get("item").asString)
        val resultItem = BuiltInRegistries.ITEM.getValue(resultItemId)
        val resultCount = (if (result.has("count")) result.get("count").asInt else 1)
        return MaceratorRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, ItemStack(resultItem, resultCount), count)
    }

    private fun read(buf: RegistryFriendlyByteBuf): MaceratorRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        return MaceratorRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, output, inputCount)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: MaceratorRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
        buf.writeVarInt(recipe.inputCount)
    }

    override fun codec(): MapCodec<MaceratorRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:macerating",
        { id, json -> fromJson(id, json) },
        { json, recipe ->
            json.add("ingredient", ic2_120.content.recipes.ingredientJson26(recipe.ingredient).apply {
                if (recipe.inputCount != 1) addProperty("count", recipe.inputCount)
            })
            json.add("result", ic2_120.content.recipes.stackJson26(recipe.output))
        }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

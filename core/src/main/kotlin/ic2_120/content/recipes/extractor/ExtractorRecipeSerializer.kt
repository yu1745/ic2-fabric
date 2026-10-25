package ic2_120.content.recipes.extractor

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

@ModMachineRecipe(id = "extracting", recipeClass = ExtractorRecipe::class)
object ExtractorRecipeSerializer : ic2_120.content.recipes.RecipeSerializerHolder26<ExtractorRecipe>() {
    private fun fromJson(id: Identifier, json: JsonObject): ExtractorRecipe {
        val ingredient = INGREDIENT_CODEC(json.getAsJsonObject("ingredient"))
        val result = json.getAsJsonObject("result")
        val itemId = Identifier.parse(result.get("item").asString)
        val item = BuiltInRegistries.ITEM.getValue(itemId)
        val count = (if (result.has("count")) result.get("count").asInt else 1)
        return ExtractorRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, ItemStack(item, count))
    }

    private fun read(buf: RegistryFriendlyByteBuf): ExtractorRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        return ExtractorRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, output)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: ExtractorRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
    }

    override fun codec(): MapCodec<ExtractorRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:extracting",
        { id, json -> fromJson(id, json) },
        { json, recipe ->
            json.add("ingredient", ic2_120.content.recipes.ingredientJson26(recipe.ingredient))
            json.add("result", ic2_120.content.recipes.stackJson26(recipe.output))
        }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ExtractorRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

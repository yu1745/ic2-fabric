package ic2_120.content.recipes.extractor

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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "extracting", recipeClass = ExtractorRecipe::class)
object ExtractorRecipeSerializer : RecipeSerializer<ExtractorRecipe> {
    private fun fromJson(id: ResourceLocation, json: JsonObject): ExtractorRecipe {
        val ingredient = INGREDIENT_CODEC(GsonHelper.getObject(json, "ingredient"))
        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val item = BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)
        return ExtractorRecipe(id, ingredient, ItemStack(item, count))
    }

    private fun read(buf: RegistryFriendlyByteBuf): ExtractorRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val output = bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        return ExtractorRecipe(id, ingredient, output)
    }

    private fun write(buf: FriendlyByteBuf, recipe: ExtractorRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
    }
}

    override fun codec(): MapCodec<ExtractorRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:extracting",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ExtractorRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })

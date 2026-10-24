package ic2_120.content.recipes.macerator

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

@ModMachineRecipe(id = "macerating", recipeClass = MaceratorRecipe::class)
object MaceratorRecipeSerializer : RecipeSerializer<MaceratorRecipe> {
    private fun fromJson(id: ResourceLocation, json: JsonObject): MaceratorRecipe {
        val ingredientJson = GsonHelper.getObject(json, "ingredient")
        val count = GsonHelper.getInt(ingredientJson, "count", 1)
        val ingredient = INGREDIENT_CODEC(ingredientJson)

        val result = GsonHelper.getObject(json, "result")
        val resultItemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val resultItem = BuiltInRegistries.ITEM.getValue(resultItemId)
        val resultCount = GsonHelper.getInt(result, "count", 1)
        return MaceratorRecipe(id, ingredient, ItemStack(resultItem, resultCount), count)
    }

    private fun read(buf: RegistryFriendlyByteBuf): MaceratorRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val output = bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        return MaceratorRecipe(id, ingredient, output, inputCount)
    }

    private fun write(buf: FriendlyByteBuf, recipe: MaceratorRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
        buf.writeVarInt(recipe.inputCount)
    }
}

    override fun codec(): MapCodec<MaceratorRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:macerating",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })

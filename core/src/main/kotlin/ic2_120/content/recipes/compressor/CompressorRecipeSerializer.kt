package ic2_120.content.recipes.compressor

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

@ModMachineRecipe(id = "compressing", recipeClass = CompressorRecipe::class)
object CompressorRecipeSerializer : RecipeSerializer<CompressorRecipe> {
    private fun fromJson(id: ResourceLocation, json: JsonObject): CompressorRecipe {
        val ingredient = INGREDIENT_CODEC(GsonHelper.getObject(json, "ingredient"))
        val inputCount = GsonHelper.getInt(json, "input_count", 1)
        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val item = BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)
        val containerReturn = if (json.has("container_return")) {
            val containerObj = json.getAsJsonObject("container_return")
            val containerId = ResourceLocation.parse(GsonHelper.getString(containerObj, "item"))
            val containerCount = GsonHelper.getInt(containerObj, "count", 1)
            ItemStack(BuiltInRegistries.ITEM.getValue(containerId), containerCount)
        } else ItemStack.EMPTY
        return CompressorRecipe(id, ingredient, inputCount, ItemStack(item, count), containerReturn)
    }

    private fun read(buf: RegistryFriendlyByteBuf): CompressorRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        val output = bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        val containerReturn = bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        return CompressorRecipe(id, ingredient, inputCount, output, containerReturn)
    }

    private fun write(buf: FriendlyByteBuf, recipe: CompressorRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        buf.writeVarInt(recipe.inputCount)
        bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
        bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.containerReturn.copy())
    }
}

    override fun codec(): MapCodec<CompressorRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:compressing",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, CompressorRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })

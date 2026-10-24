package ic2_120.content.recipes.compressor

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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "compressing", recipeClass = CompressorRecipe::class)
object CompressorRecipeSerializer : RecipeSerializer<CompressorRecipe> {
    private fun fromJson(id: ResourceLocation, json: JsonObject): CompressorRecipe {
        val ingredient = INGREDIENT_CODEC(json.getAsJsonObject("ingredient"))
        val inputCount = (if (json.has("input_count")) json.get("input_count").asInt else 1)
        val result = json.getAsJsonObject("result")
        val itemId = ResourceLocation.parse(result.get("item").asString)
        val item = BuiltInRegistries.ITEM.get(itemId)
        val count = (if (result.has("count")) result.get("count").asInt else 1)
        val containerReturn = if (json.has("container_return")) {
            val containerObj = json.getAsJsonObject("container_return")
            val containerId = ResourceLocation.parse(containerObj.get("item").asString)
            val containerCount = (if (containerObj.has("count")) containerObj.get("count").asInt else 1)
            ItemStack(BuiltInRegistries.ITEM.get(containerId), containerCount)
        } else ItemStack.EMPTY
        return CompressorRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, inputCount, ItemStack(item, count), containerReturn)
    }

    private fun read(buf: RegistryFriendlyByteBuf): CompressorRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        val output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        val containerReturn = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        return CompressorRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, inputCount, output, containerReturn)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: CompressorRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        buf.writeVarInt(recipe.inputCount)
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.containerReturn.copy())
    }

    override fun codec(): MapCodec<CompressorRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:compressing",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, CompressorRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

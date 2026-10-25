package ic2_120.content.recipes.centrifuge

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.content.recipes.INGREDIENT_CODEC
import ic2_120.content.recipes.RecipeCodecBridge

import net.minecraft.network.codec.StreamCodec

import net.minecraft.network.RegistryFriendlyByteBuf

import com.mojang.serialization.MapCodec

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

/**
 * 热能离心机配方序列化器
 *
 * JSON 格式：
 * {
 *   "type": "ic2_120:centrifuging",
 *   "ingredient": { "item": "minecraft:cobblestone" },
 *   "input_count": 1,
 *   "min_heat": 100,
 *   "results": [
 *     { "item": "ic2_120:stone_dust", "count": 1 },
 *     { "item": "ic2_120:copper_dust", "count": 2 }
 *   ]
 * }
 */
@ModMachineRecipe(id = "centrifuging", recipeClass = CentrifugeRecipe::class)
object CentrifugeRecipeSerializer : RecipeSerializer<CentrifugeRecipe> {
    private fun fromJson(id: ResourceLocation, json: JsonObject): CentrifugeRecipe {
        val ingredient = INGREDIENT_CODEC(json.getAsJsonObject("ingredient"))
        val inputCount = (if (json.has("input_count")) json.get("input_count").asInt else 1)
        val minHeat = json.get("min_heat").asInt

        val resultsArray = json.getAsJsonArray("results")
        val outputs = mutableListOf<ItemStack>()

        resultsArray.forEach { element ->
            val resultObj = element.asJsonObject
            val itemId = ResourceLocation.parse(resultObj.get("item").asString)
            val item = BuiltInRegistries.ITEM.get(itemId)
            val count = (if (resultObj.has("count")) resultObj.get("count").asInt else 1)
            outputs.add(ItemStack(item, count))
        }

        return CentrifugeRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, inputCount, minHeat, outputs)
    }

    private fun read(buf: RegistryFriendlyByteBuf): CentrifugeRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        val minHeat = buf.readVarInt()

        val outputCount = buf.readVarInt()
        val outputs = mutableListOf<ItemStack>()

        repeat(outputCount) {
            outputs.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf))
        }

        return CentrifugeRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, inputCount, minHeat, outputs)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: CentrifugeRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        buf.writeVarInt(recipe.inputCount)
        buf.writeVarInt(recipe.minHeat)

        buf.writeVarInt(recipe.outputs.size)
        recipe.outputs.forEach { output ->
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, output.copy())
        }
    }

    override fun codec(): MapCodec<CentrifugeRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:centrifuging",
        { id, json -> fromJson(id, json) },
        { json, recipe ->
            json.add("ingredient", RecipeCodecBridge.ingredientJson(recipe.ingredient))
            json.addProperty("input_count", recipe.inputCount)
            json.addProperty("min_heat", recipe.minHeat)
            json.add("results", com.google.gson.JsonArray().apply {
                recipe.outputs.forEach { add(RecipeCodecBridge.stackJson(it)) }
            })
        }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

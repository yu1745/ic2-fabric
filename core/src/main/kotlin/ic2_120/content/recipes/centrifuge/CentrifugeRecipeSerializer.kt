package ic2_120.content.recipes.centrifuge

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
import net.minecraft.core.registries.BuiltInRegistries
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
        val ingredient = INGREDIENT_CODEC(GsonHelper.getObject(json, "ingredient"))
        val inputCount = GsonHelper.getInt(json, "input_count", 1)
        val minHeat = GsonHelper.getInt(json, "min_heat")

        val resultsArray = GsonHelper.getArray(json, "results")
        val outputs = mutableListOf<ItemStack>()

        resultsArray.forEach { element ->
            val resultObj = element.asJsonObject
            val itemId = ResourceLocation.parse(GsonHelper.getString(resultObj, "item"))
            val item = BuiltInRegistries.ITEM.get(itemId)
            val count = GsonHelper.getInt(resultObj, "count", 1)
            outputs.add(ItemStack(item, count))
        }

        return CentrifugeRecipe(id, ingredient, inputCount, minHeat, outputs)
    }

    private fun read(buf: RegistryFriendlyByteBuf): CentrifugeRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        val minHeat = buf.readVarInt()

        val outputCount = buf.readVarInt()
        val outputs = mutableListOf<ItemStack>()

        repeat(outputCount) {
            outputs.add(bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf))
        }

        return CentrifugeRecipe(id, ingredient, inputCount, minHeat, outputs)
    }

    private fun write(buf: FriendlyByteBuf, recipe: CentrifugeRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        buf.writeVarInt(recipe.inputCount)
        buf.writeVarInt(recipe.minHeat)

        buf.writeVarInt(recipe.outputs.size)
        recipe.outputs.forEach { output ->
            bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, output.copy())
        }
    }
}

    override fun codec(): MapCodec<CentrifugeRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:centrifuging",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })

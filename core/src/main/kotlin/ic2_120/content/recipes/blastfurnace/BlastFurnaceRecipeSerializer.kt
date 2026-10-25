package ic2_120.content.recipes.blastfurnace

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

/**
 * 高炉配方序列化器
 *
 * JSON 格式：
 * {
 *   "type": "ic2_120:blast_furnacing",
 *   "ingredient": { "item": "minecraft:iron_ingot" },
 *   "steel_output": { "item": "ic2_120:steel_ingot", "count": 1 },
 *   "slag_output": { "item": "ic2_120:slag", "count": 1 }
 * }
 */
@ModMachineRecipe(id = "blast_furnacing", recipeClass = BlastFurnaceRecipe::class)
object BlastFurnaceRecipeSerializer : ic2_120.content.recipes.RecipeSerializerHolder26<BlastFurnaceRecipe>() {
    private fun fromJson(id: Identifier, json: JsonObject): BlastFurnaceRecipe {
        val ingredient = INGREDIENT_CODEC(json.getAsJsonObject("ingredient"))

        val steelOutputJson = json.getAsJsonObject("steel_output")
        val steelItemId = Identifier.parse(steelOutputJson.get("item").asString)
        val steelItem = BuiltInRegistries.ITEM.getValue(steelItemId)
        val steelCount = (if (steelOutputJson.has("count")) steelOutputJson.get("count").asInt else 1)
        val steelOutput = ItemStack(steelItem, steelCount)

        val slagOutputJson = json.getAsJsonObject("slag_output")
        val slagItemId = Identifier.parse(slagOutputJson.get("item").asString)
        val slagItem = BuiltInRegistries.ITEM.getValue(slagItemId)
        val slagCount = (if (slagOutputJson.has("count")) slagOutputJson.get("count").asInt else 1)
        val slagOutput = ItemStack(slagItem, slagCount)

        return BlastFurnaceRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, steelOutput, slagOutput)
    }

    private fun read(buf: RegistryFriendlyByteBuf): BlastFurnaceRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val steelOutput = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        val slagOutput = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)

        return BlastFurnaceRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, steelOutput, slagOutput)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: BlastFurnaceRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.steelOutput.copy())
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.slagOutput.copy())
    }

    override fun codec(): MapCodec<BlastFurnaceRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:blast_furnacing",
        { id, json -> fromJson(id, json) },
        { json, recipe ->
            json.add("ingredient", ic2_120.content.recipes.ingredientJson26(recipe.ingredient))
            json.add("steel_output", ic2_120.content.recipes.stackJson26(recipe.steelOutput))
            json.add("slag_output", ic2_120.content.recipes.stackJson26(recipe.slagOutput))
        }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

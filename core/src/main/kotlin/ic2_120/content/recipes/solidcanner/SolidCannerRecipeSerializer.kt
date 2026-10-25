package ic2_120.content.recipes.solidcanner

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

@ModMachineRecipe(id = "solid_canning", recipeClass = SolidCannerRecipe::class)
object SolidCannerRecipeSerializer : ic2_120.content.recipes.RecipeSerializerHolder26<SolidCannerRecipe>() {

    private fun fromJson(id: Identifier, json: JsonObject): SolidCannerRecipe {
        val slot0Ingredient = INGREDIENT_CODEC(json.getAsJsonObject("slot0_ingredient"))
        val slot0Count = json.get("slot0_count").asInt
        val slot1Ingredient = INGREDIENT_CODEC(json.getAsJsonObject("slot1_ingredient"))
        val slot1Count = json.get("slot1_count").asInt
        val result = json.getAsJsonObject("result")
        val itemId = Identifier.parse(result.get("item").asString)
        val item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(itemId)
        val count = (if (result.has("count")) result.get("count").asInt else 1)
        return SolidCannerRecipe(RecipeCodecBridge.DUMMY_ID, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, ItemStack(item, count))
    }

    private fun read(buf: RegistryFriendlyByteBuf): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val slot0Count = buf.readInt()
        val slot1Ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val slot1Count = buf.readInt()
        val output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        return SolidCannerRecipe(RecipeCodecBridge.DUMMY_ID, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, output)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: SolidCannerRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.slot0Ingredient)
        buf.writeInt(recipe.slot0Count)
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.slot1Ingredient)
        buf.writeInt(recipe.slot1Count)
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
    }

    override fun codec(): MapCodec<SolidCannerRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:solid_canning",
        { id, json -> fromJson(id, json) },
        { json, recipe ->
            json.add("slot0_ingredient", ic2_120.content.recipes.ingredientJson26(recipe.slot0Ingredient))
            json.addProperty("slot0_count", recipe.slot0Count)
            json.add("slot1_ingredient", ic2_120.content.recipes.ingredientJson26(recipe.slot1Ingredient))
            json.addProperty("slot1_count", recipe.slot1Count)
            json.add("result", ic2_120.content.recipes.stackJson26(recipe.output))
        }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, SolidCannerRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

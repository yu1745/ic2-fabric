package ic2_120.content.recipes.blockcutter

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
 * 方块切割机配方序列化器
 *
 * JSON 格式：
 * {
 *   "type": "ic2_120:cutting",
 *   "ingredient": { "item": "minecraft:oak_planks" },
 *   "input_count": 1,
 *   "material_hardness": 2.0,
 *   "result": { "item": "minecraft:oak_slab", "count": 9 }
 * }
 */
@ModMachineRecipe(id = "cutting", recipeClass = BlockCutterRecipe::class)
object BlockCutterRecipeSerializer : ic2_120.content.recipes.RecipeSerializerHolder26<BlockCutterRecipe>() {
    private fun fromJson(id: Identifier, json: JsonObject): BlockCutterRecipe {
        val ingredient = INGREDIENT_CODEC(json.getAsJsonObject("ingredient"))
        val inputCount = (if (json.has("input_count")) json.get("input_count").asInt else 1)
        val materialHardness = json.get("material_hardness").asFloat
        val result = json.getAsJsonObject("result")
        val itemId = Identifier.parse(result.get("item").asString)
        val item = BuiltInRegistries.ITEM.getValue(itemId)
        val count = (if (result.has("count")) result.get("count").asInt else 1)

        return BlockCutterRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, inputCount, materialHardness, ItemStack(item, count))
    }

    private fun read(buf: RegistryFriendlyByteBuf): BlockCutterRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        val materialHardness = buf.readFloat()
        val output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)

        return BlockCutterRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, inputCount, materialHardness, output)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: BlockCutterRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        buf.writeVarInt(recipe.inputCount)
        buf.writeFloat(recipe.materialHardness)
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
    }

    override fun codec(): MapCodec<BlockCutterRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:cutting",
        { id, json -> fromJson(id, json) },
        { json, recipe ->
            json.add("ingredient", ic2_120.content.recipes.ingredientJson26(recipe.ingredient))
            json.addProperty("input_count", recipe.inputCount)
            json.addProperty("material_hardness", recipe.materialHardness)
            json.add("result", ic2_120.content.recipes.stackJson26(recipe.output))
        }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, BlockCutterRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

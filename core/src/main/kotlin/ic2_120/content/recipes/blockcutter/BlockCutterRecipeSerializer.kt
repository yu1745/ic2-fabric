package ic2_120.content.recipes.blockcutter

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
object BlockCutterRecipeSerializer : RecipeSerializer<BlockCutterRecipe> {
    private fun fromJson(id: ResourceLocation, json: JsonObject): BlockCutterRecipe {
        val ingredient = INGREDIENT_CODEC(GsonHelper.getObject(json, "ingredient"))
        val inputCount = GsonHelper.getInt(json, "input_count", 1)
        val materialHardness = GsonHelper.getFloat(json, "material_hardness")
        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val item = BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)

        return BlockCutterRecipe(id, ingredient, inputCount, materialHardness, ItemStack(item, count))
    }

    private fun read(buf: RegistryFriendlyByteBuf): BlockCutterRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val inputCount = buf.readVarInt()
        val materialHardness = buf.readFloat()
        val output = bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf)

        return BlockCutterRecipe(id, ingredient, inputCount, materialHardness, output)
    }

    private fun write(buf: FriendlyByteBuf, recipe: BlockCutterRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        buf.writeVarInt(recipe.inputCount)
        buf.writeFloat(recipe.materialHardness)
        bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
    }
}

    override fun codec(): MapCodec<BlockCutterRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:cutting",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, BlockCutterRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })

package ic2_120.content.recipes.orewashing

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
 * 洗矿机配方序列化器
 *
 * JSON 格式：
 * {
 *   "type": "ic2_120:ore_washing",
 *   "ingredient": { "item": "ic2_120:crushed_iron" },
 *   "outputs": [
 *     { "item": "ic2_120:purified_iron", "count": 1 },
 *     { "item": "ic2_120:stone_dust", "count": 1 },
 *     { "item": "ic2_120:small_iron_dust", "count": 2 }
 *   ],
 *   "water_consumption_mb": 1000
 * }
 */
@ModMachineRecipe(id = "ore_washing", recipeClass = OreWashingRecipe::class)
object OreWashingRecipeSerializer : RecipeSerializer<OreWashingRecipe> {
    private fun fromJson(id: ResourceLocation, json: JsonObject): OreWashingRecipe {
        val ingredient = INGREDIENT_CODEC(json.getAsJsonObject("ingredient"))

        val outputsArray = json.getAsJsonArray("outputs")
        val outputs = outputsArray.map { element ->
            val outputObj = element.asJsonObject
            val itemId = ResourceLocation.parse(outputObj.get("item").asString)
            val item = BuiltInRegistries.ITEM.get(itemId)
            val count = (if (outputObj.has("count")) outputObj.get("count").asInt else 1)
            ItemStack(item, count)
        }

        // 读取旧 mB 键并转为 droplets（兼容旧 JSON）
        val waterMb = (if (json.has("water_consumption_mb")) json.get("water_consumption_mb").asLong else 1000L)
        val waterConsumption = waterMb * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET / 1000L

        return OreWashingRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, outputs, waterConsumption)
    }

    private fun read(buf: RegistryFriendlyByteBuf): OreWashingRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)

        val outputCount = buf.readVarInt()
        val outputs = (0 until outputCount).map {
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        }

        val waterConsumption = buf.readLong()

        return OreWashingRecipe(RecipeCodecBridge.DUMMY_ID, ingredient, outputs, waterConsumption)
    }

    private fun write(buf: RegistryFriendlyByteBuf, recipe: OreWashingRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)

        buf.writeVarInt(recipe.outputItems.size)
        recipe.outputItems.forEach { output ->
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, output.copy())
        }

        buf.writeLong(recipe.waterConsumptionDroplets)
    }

    override fun codec(): MapCodec<OreWashingRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:ore_washing",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, OreWashingRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })
}

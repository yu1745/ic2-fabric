package ic2_120.content.recipes.orewashing

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack

import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

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
    override fun read(id: Identifier, json: JsonObject): OreWashingRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))

        val outputsArray = JsonHelper.getArray(json, "outputs")
        val outputs = outputsArray.map { element ->
            val outputObj = element.asJsonObject
            val itemId = Identifier(JsonHelper.getString(outputObj, "item"))
            val item = Registries.ITEM.get(itemId)
            val count = JsonHelper.getInt(outputObj, "count", 1)
            ItemStack(item, count)
        }

        val waterConsumption = JsonHelper.getLong(json, "water_consumption_mb", 1000L)

        return OreWashingRecipe(id, ingredient, outputs, waterConsumption)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): OreWashingRecipe {
        val ingredient = Ingredient.PACKET_CODEC.decode(buf)

        val outputCount = buf.readVarInt()
        val outputs = (0 until outputCount).map {
            ItemStack.PACKET_CODEC.decode(buf)
        }

        val waterConsumption = buf.readLong()

        return OreWashingRecipe(id, ingredient, outputs, waterConsumption)
    }

    override fun write(buf: PacketByteBuf, recipe: OreWashingRecipe) {
        Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)

        buf.writeVarInt(recipe.outputItems.size)
        recipe.outputItems.forEach { output ->
            ItemStack.PACKET_CODEC.encode(buf, output.copy())
        }

        buf.writeLong(recipe.waterConsumptionMb)
    }
}

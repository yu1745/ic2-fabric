package ic2_120.content.recipes.centrifuge

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
    override fun read(id: ResourceLocation, json: JsonObject): CentrifugeRecipe {
        val ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "ingredient"))
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

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): CentrifugeRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val inputCount = buf.readVarInt()
        val minHeat = buf.readVarInt()

        val outputCount = buf.readVarInt()
        val outputs = mutableListOf<ItemStack>()

        repeat(outputCount) {
            outputs.add(buf.readItemStack())
        }

        return CentrifugeRecipe(id, ingredient, inputCount, minHeat, outputs)
    }

    override fun write(buf: FriendlyByteBuf, recipe: CentrifugeRecipe) {
        recipe.ingredient.write(buf)
        buf.writeVarInt(recipe.inputCount)
        buf.writeVarInt(recipe.minHeat)

        buf.writeVarInt(recipe.outputs.size)
        recipe.outputs.forEach { output ->
            buf.writeItemStack(output.copy())
        }
    }
}

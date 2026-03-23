package ic2_120.content.recipes.blastfurnace

import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

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
object BlastFurnaceRecipeSerializer : RecipeSerializer<BlastFurnaceRecipe> {
    override fun read(id: Identifier, json: JsonObject): BlastFurnaceRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))

        val steelOutputJson = JsonHelper.getObject(json, "steel_output")
        val steelItemId = Identifier(JsonHelper.getString(steelOutputJson, "item"))
        val steelItem = Registries.ITEM.get(steelItemId)
        val steelCount = JsonHelper.getInt(steelOutputJson, "count", 1)
        val steelOutput = ItemStack(steelItem, steelCount)

        val slagOutputJson = JsonHelper.getObject(json, "slag_output")
        val slagItemId = Identifier(JsonHelper.getString(slagOutputJson, "item"))
        val slagItem = Registries.ITEM.get(slagItemId)
        val slagCount = JsonHelper.getInt(slagOutputJson, "count", 1)
        val slagOutput = ItemStack(slagItem, slagCount)

        return BlastFurnaceRecipe(id, ingredient, steelOutput, slagOutput)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): BlastFurnaceRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val steelOutput = buf.readItemStack()
        val slagOutput = buf.readItemStack()

        return BlastFurnaceRecipe(id, ingredient, steelOutput, slagOutput)
    }

    override fun write(buf: PacketByteBuf, recipe: BlastFurnaceRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.steelOutput.copy())
        buf.writeItemStack(recipe.slagOutput.copy())
    }
}

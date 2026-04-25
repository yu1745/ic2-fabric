package ic2_120.content.recipes.blastfurnace

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack

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
@ModMachineRecipe(id = "blast_furnacing", recipeClass = BlastFurnaceRecipe::class)
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
        val ingredient = Ingredient.PACKET_CODEC.decode(buf)
        val steelOutput = ItemStack.PACKET_CODEC.decode(buf)
        val slagOutput = ItemStack.PACKET_CODEC.decode(buf)

        return BlastFurnaceRecipe(id, ingredient, steelOutput, slagOutput)
    }

    override fun write(buf: PacketByteBuf, recipe: BlastFurnaceRecipe) {
        Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
        ItemStack.PACKET_CODEC.encode(buf, recipe.steelOutput.copy())
        ItemStack.PACKET_CODEC.encode(buf, recipe.slagOutput.copy())
    }
}

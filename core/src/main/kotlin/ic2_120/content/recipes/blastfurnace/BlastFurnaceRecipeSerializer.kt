package ic2_120.content.recipes.blastfurnace

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
    override fun read(id: ResourceLocation, json: JsonObject): BlastFurnaceRecipe {
        val ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "ingredient"))

        val steelOutputJson = GsonHelper.getObject(json, "steel_output")
        val steelItemId = ResourceLocation(GsonHelper.getString(steelOutputJson, "item"))
        val steelItem = BuiltInRegistries.ITEM.get(steelItemId)
        val steelCount = GsonHelper.getInt(steelOutputJson, "count", 1)
        val steelOutput = ItemStack(steelItem, steelCount)

        val slagOutputJson = GsonHelper.getObject(json, "slag_output")
        val slagItemId = ResourceLocation(GsonHelper.getString(slagOutputJson, "item"))
        val slagItem = BuiltInRegistries.ITEM.get(slagItemId)
        val slagCount = GsonHelper.getInt(slagOutputJson, "count", 1)
        val slagOutput = ItemStack(slagItem, slagCount)

        return BlastFurnaceRecipe(id, ingredient, steelOutput, slagOutput)
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): BlastFurnaceRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val steelOutput = buf.readItemStack()
        val slagOutput = buf.readItemStack()

        return BlastFurnaceRecipe(id, ingredient, steelOutput, slagOutput)
    }

    override fun write(buf: FriendlyByteBuf, recipe: BlastFurnaceRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.steelOutput.copy())
        buf.writeItemStack(recipe.slagOutput.copy())
    }
}

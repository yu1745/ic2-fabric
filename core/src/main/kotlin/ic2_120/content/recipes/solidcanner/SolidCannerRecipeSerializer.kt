package ic2_120.content.recipes.solidcanner

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

@ModMachineRecipe(id = "solid_canning", recipeClass = SolidCannerRecipe::class)
object SolidCannerRecipeSerializer : RecipeSerializer<SolidCannerRecipe> {

    override fun read(id: Identifier, json: JsonObject): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "slot0_ingredient"))
        val slot0Count = JsonHelper.getInt(json, "slot0_count")
        val slot1Ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "slot1_ingredient"))
        val slot1Count = JsonHelper.getInt(json, "slot1_count")
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = net.minecraft.registry.Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, ItemStack(item, count))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.fromPacket(buf)
        val slot0Count = buf.readInt()
        val slot1Ingredient = Ingredient.fromPacket(buf)
        val slot1Count = buf.readInt()
        val output = buf.readItemStack()
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, output)
    }

    override fun write(buf: PacketByteBuf, recipe: SolidCannerRecipe) {
        recipe.slot0Ingredient.write(buf)
        buf.writeInt(recipe.slot0Count)
        recipe.slot1Ingredient.write(buf)
        buf.writeInt(recipe.slot1Count)
        buf.writeItemStack(recipe.output.copy())
    }
}

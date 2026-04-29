package ic2_120.content.recipes.macerator

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

@ModMachineRecipe(id = "macerating", recipeClass = MaceratorRecipe::class)
object MaceratorRecipeSerializer : RecipeSerializer<MaceratorRecipe> {
    override fun read(id: Identifier, json: JsonObject): MaceratorRecipe {
        val ingredientJson = JsonHelper.getObject(json, "ingredient")
        val item = Identifier(JsonHelper.getString(ingredientJson, "item"))
        val count = JsonHelper.getInt(ingredientJson, "count", 1)

        // 创建支持堆叠数量的Ingredient
        val ingredient = if (count == 1) {
            Ingredient.fromJson(ingredientJson)
        } else {
            // 对于多个物品，我们使用Ingredient.of()并传入多个ItemStack
            val itemEntry = Registries.ITEM.get(item)
            val stacks = (1..count).map { net.minecraft.item.ItemStack(itemEntry) }
            Ingredient.ofStacks(*stacks.toTypedArray())
        }

        val result = JsonHelper.getObject(json, "result")
        val resultItemId = Identifier(JsonHelper.getString(result, "item"))
        val resultItem = Registries.ITEM.get(resultItemId)
        val resultCount = JsonHelper.getInt(result, "count", 1)
        return MaceratorRecipe(id, ingredient, ItemStack(resultItem, resultCount))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): MaceratorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        return MaceratorRecipe(id, ingredient, output)
    }

    override fun write(buf: PacketByteBuf, recipe: MaceratorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())
    }
}

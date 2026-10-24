package ic2_120.content.recipes.solidcanner

import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "solid_canning", recipeClass = SolidCannerRecipe::class)
object SolidCannerRecipeSerializer : RecipeSerializer<SolidCannerRecipe> {

    override fun read(id: ResourceLocation, json: JsonObject): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "slot0_ingredient"))
        val slot0Count = GsonHelper.getInt(json, "slot0_count")
        val slot1Ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "slot1_ingredient"))
        val slot1Count = GsonHelper.getInt(json, "slot1_count")
        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation(GsonHelper.getString(result, "item"))
        val item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, ItemStack(item, count))
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.fromPacket(buf)
        val slot0Count = buf.readInt()
        val slot1Ingredient = Ingredient.fromPacket(buf)
        val slot1Count = buf.readInt()
        val output = buf.readItemStack()
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, output)
    }

    override fun write(buf: FriendlyByteBuf, recipe: SolidCannerRecipe) {
        recipe.slot0Ingredient.write(buf)
        buf.writeInt(recipe.slot0Count)
        recipe.slot1Ingredient.write(buf)
        buf.writeInt(recipe.slot1Count)
        buf.writeItemStack(recipe.output.copy())
    }
}

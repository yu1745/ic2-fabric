package ic2_120.content.recipes.metalformer

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
 * 金属成型机配方序列化器（三种模式共用）
 */
@ModMachineRecipe(id = "metal_forming", recipeClass = MetalFormerRecipe::class)
object MetalFormerRecipeSerializer : RecipeSerializer<MetalFormerRecipe> {
    override fun read(id: ResourceLocation, json: JsonObject): MetalFormerRecipe {
        val ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "ingredient"))

        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val item = BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)
        val output = ItemStack(item, count)

        val mode = GsonHelper.getString(json, "mode", "rolling")

        return when (mode?.lowercase()) {
            "cutting" -> CuttingRecipe(id, ingredient, output)
            "extruding" -> ExtrudingRecipe(id, ingredient, output)
            else -> RollingRecipe(id, ingredient, output)  // 默认是辊压模式
        }
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): MetalFormerRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        val mode = buf.readString()

        return when (mode) {
            "cutting" -> CuttingRecipe(id, ingredient, output)
            "extruding" -> ExtrudingRecipe(id, ingredient, output)
            else -> RollingRecipe(id, ingredient, output)
        }
    }

    override fun write(buf: FriendlyByteBuf, recipe: MetalFormerRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())

        val mode = when (recipe) {
            is CuttingRecipe -> "cutting"
            is ExtrudingRecipe -> "extruding"
            else -> "rolling"
        }
        buf.writeString(mode)
    }
}

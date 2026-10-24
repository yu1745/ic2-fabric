package ic2_120.content.recipes.orewashing

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
    override fun read(id: ResourceLocation, json: JsonObject): OreWashingRecipe {
        val ingredient = Ingredient.fromJson(GsonHelper.getObject(json, "ingredient"))

        val outputsArray = GsonHelper.getArray(json, "outputs")
        val outputs = outputsArray.map { element ->
            val outputObj = element.asJsonObject
            val itemId = ResourceLocation.parse(GsonHelper.getString(outputObj, "item"))
            val item = BuiltInRegistries.ITEM.get(itemId)
            val count = GsonHelper.getInt(outputObj, "count", 1)
            ItemStack(item, count)
        }

        // 读取旧 mB 键并转为 droplets（兼容旧 JSON）
        val waterMb = GsonHelper.getLong(json, "water_consumption_mb", 1000L)
        val waterConsumption = waterMb * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET / 1000L

        return OreWashingRecipe(id, ingredient, outputs, waterConsumption)
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): OreWashingRecipe {
        val ingredient = Ingredient.fromPacket(buf)

        val outputCount = buf.readVarInt()
        val outputs = (0 until outputCount).map {
            buf.readItemStack()
        }

        val waterConsumption = buf.readLong()

        return OreWashingRecipe(id, ingredient, outputs, waterConsumption)
    }

    override fun write(buf: FriendlyByteBuf, recipe: OreWashingRecipe) {
        recipe.ingredient.write(buf)

        buf.writeVarInt(recipe.outputItems.size)
        recipe.outputItems.forEach { output ->
            buf.writeItemStack(output.copy())
        }

        buf.writeLong(recipe.waterConsumptionDroplets)
    }
}

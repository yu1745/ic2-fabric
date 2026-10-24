package ic2_120.content.recipes.metalformer

import ic2_120.content.recipes.RecipeCodecBridge

import net.minecraft.network.codec.StreamCodec

import net.minecraft.network.RegistryFriendlyByteBuf

import com.mojang.serialization.MapCodec

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
    private fun fromJson(id: ResourceLocation, json: JsonObject): MetalFormerRecipe {
        val ingredient = INGREDIENT_CODEC(GsonHelper.getObject(json, "ingredient"))

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

    private fun read(buf: RegistryFriendlyByteBuf): MetalFormerRecipe {
        val ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val output = bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        val mode = buf.readString()

        return when (mode) {
            "cutting" -> CuttingRecipe(id, ingredient, output)
            "extruding" -> ExtrudingRecipe(id, ingredient, output)
            else -> RollingRecipe(id, ingredient, output)
        }
    }

    private fun write(buf: FriendlyByteBuf, recipe: MetalFormerRecipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient)
        bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())

        val mode = when (recipe) {
            is CuttingRecipe -> "cutting"
            is ExtrudingRecipe -> "extruding"
            else -> "rolling"
        }
        buf.writeString(mode)
    }
}

    override fun codec(): MapCodec<MetalFormerRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:metal_forming",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, MetalFormerRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })

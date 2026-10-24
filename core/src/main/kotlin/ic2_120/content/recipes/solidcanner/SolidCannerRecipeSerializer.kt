package ic2_120.content.recipes.solidcanner

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
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper

@ModMachineRecipe(id = "solid_canning", recipeClass = SolidCannerRecipe::class)
object SolidCannerRecipeSerializer : RecipeSerializer<SolidCannerRecipe> {

    private fun fromJson(id: ResourceLocation, json: JsonObject): SolidCannerRecipe {
        val slot0Ingredient = INGREDIENT_CODEC(GsonHelper.getObject(json, "slot0_ingredient"))
        val slot0Count = GsonHelper.getInt(json, "slot0_count")
        val slot1Ingredient = INGREDIENT_CODEC(GsonHelper.getObject(json, "slot1_ingredient"))
        val slot1Count = GsonHelper.getInt(json, "slot1_count")
        val result = GsonHelper.getObject(json, "result")
        val itemId = ResourceLocation.parse(GsonHelper.getString(result, "item"))
        val item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)
        val count = GsonHelper.getInt(result, "count", 1)
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, ItemStack(item, count))
    }

    private fun read(buf: RegistryFriendlyByteBuf): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val slot0Count = buf.readInt()
        val slot1Ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
        val slot1Count = buf.readInt()
        val output = bufItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, output)
    }

    private fun write(buf: FriendlyByteBuf, recipe: SolidCannerRecipe) {
        recipe.slot0Ingredient.write(buf)
        buf.writeInt(recipe.slot0Count)
        recipe.slot1Ingredient.write(buf)
        buf.writeInt(recipe.slot1Count)
        bufItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.output.copy())
    }
}

    override fun codec(): MapCodec<SolidCannerRecipe> = RecipeCodecBridge.jsonMapCodec(
        "ic2_120:solid_canning",
        { id, json -> fromJson(id, json) },
        { _, _ -> throw UnsupportedOperationException("recipe JSON is produced by datagen only") }
    )

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, SolidCannerRecipe> =
        StreamCodec.ofMember({ recipe, buf -> write(buf, recipe) }, { buf -> read(buf) })

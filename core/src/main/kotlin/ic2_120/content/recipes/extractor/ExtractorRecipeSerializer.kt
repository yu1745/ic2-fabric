package ic2_120.content.recipes.extractor

<<<<<<< HEAD
import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

@ModMachineRecipe(id = "extracting", recipeClass = ExtractorRecipe::class)
object ExtractorRecipeSerializer : RecipeSerializer<ExtractorRecipe> {
    override fun read(id: Identifier, json: JsonObject): ExtractorRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        return ExtractorRecipe(id, ingredient, ItemStack(item, count))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): ExtractorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        return ExtractorRecipe(id, ingredient, output)
    }

    override fun write(buf: PacketByteBuf, recipe: ExtractorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())
    }
=======
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.util.Identifier

@ModMachineRecipe(id = "extracting", recipeClass = ExtractorRecipe::class)
object ExtractorRecipeSerializer : RecipeSerializer<ExtractorRecipe> {
    override fun codec(): MapCodec<ExtractorRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter { it.output }
        ).apply(instance) { ingredient, output ->
            ExtractorRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, ExtractorRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val output = ItemStack.PACKET_CODEC.decode(buf)
            ExtractorRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
        }
    )
>>>>>>> backup/1.21.1
}

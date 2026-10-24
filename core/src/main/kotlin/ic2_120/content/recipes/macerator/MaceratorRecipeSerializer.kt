package ic2_120.content.recipes.macerator

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

@ModMachineRecipe(id = "macerating", recipeClass = MaceratorRecipe::class)
object MaceratorRecipeSerializer : RecipeSerializer<MaceratorRecipe> {
    override fun read(id: Identifier, json: JsonObject): MaceratorRecipe {
        val ingredientJson = JsonHelper.getObject(json, "ingredient")
        val count = JsonHelper.getInt(ingredientJson, "count", 1)
        val ingredient = Ingredient.fromJson(ingredientJson)

        val result = JsonHelper.getObject(json, "result")
        val resultItemId = Identifier(JsonHelper.getString(result, "item"))
        val resultItem = Registries.ITEM.get(resultItemId)
        val resultCount = JsonHelper.getInt(result, "count", 1)
        return MaceratorRecipe(id, ingredient, ItemStack(resultItem, resultCount), count)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): MaceratorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        val inputCount = buf.readVarInt()
        return MaceratorRecipe(id, ingredient, output, inputCount)
    }

    override fun write(buf: PacketByteBuf, recipe: MaceratorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())
        buf.writeVarInt(recipe.inputCount)
    }
=======
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.util.Identifier

@ModMachineRecipe(id = "macerating", recipeClass = MaceratorRecipe::class)
object MaceratorRecipeSerializer : RecipeSerializer<MaceratorRecipe> {
    override fun codec(): MapCodec<MaceratorRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter { it.output },
            Codec.INT.optionalFieldOf("inputCount", 1).forGetter { it.inputCount }
        ).apply(instance) { ingredient, output, inputCount ->
            MaceratorRecipe(Identifier.of("ic2_120", "_"), ingredient, output, inputCount)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, MaceratorRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
            buf.writeVarInt(recipe.inputCount)
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val output = ItemStack.PACKET_CODEC.decode(buf)
            val inputCount = buf.readVarInt()
            MaceratorRecipe(Identifier.of("ic2_120", "_"), ingredient, output, inputCount)
        }
    )
>>>>>>> backup/1.21.1
}

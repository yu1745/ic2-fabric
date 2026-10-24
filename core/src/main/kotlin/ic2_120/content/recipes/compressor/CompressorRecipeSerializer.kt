package ic2_120.content.recipes.compressor

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

@ModMachineRecipe(id = "compressing", recipeClass = CompressorRecipe::class)
object CompressorRecipeSerializer : RecipeSerializer<CompressorRecipe> {
    override fun read(id: Identifier, json: JsonObject): CompressorRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))
        val inputCount = JsonHelper.getInt(json, "input_count", 1)
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        val containerReturn = if (json.has("container_return")) {
            val containerObj = json.getAsJsonObject("container_return")
            val containerId = Identifier(JsonHelper.getString(containerObj, "item"))
            val containerCount = JsonHelper.getInt(containerObj, "count", 1)
            ItemStack(Registries.ITEM.get(containerId), containerCount)
        } else ItemStack.EMPTY
        return CompressorRecipe(id, ingredient, inputCount, ItemStack(item, count), containerReturn)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): CompressorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val inputCount = buf.readVarInt()
        val output = buf.readItemStack()
        val containerReturn = buf.readItemStack()
        return CompressorRecipe(id, ingredient, inputCount, output, containerReturn)
    }

    override fun write(buf: PacketByteBuf, recipe: CompressorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeVarInt(recipe.inputCount)
        buf.writeItemStack(recipe.output.copy())
        buf.writeItemStack(recipe.containerReturn.copy())
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

@ModMachineRecipe(id = "compressing", recipeClass = CompressorRecipe::class)
object CompressorRecipeSerializer : RecipeSerializer<CompressorRecipe> {
    override fun codec(): MapCodec<CompressorRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            Codec.INT.fieldOf("input_count").orElse(1).forGetter { it.inputCount },
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter { it.output },
            ItemStack.OPTIONAL_CODEC.fieldOf("container_return").forGetter { it.containerReturn }
        ).apply(instance) { ingredient, inputCount, output, containerReturn ->
            CompressorRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, output, containerReturn)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, CompressorRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            buf.writeVarInt(recipe.inputCount)
            ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
            ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, recipe.containerReturn.copy())
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val inputCount = buf.readVarInt()
            val output = ItemStack.PACKET_CODEC.decode(buf)
            val containerReturn = ItemStack.OPTIONAL_PACKET_CODEC.decode(buf)
            CompressorRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, output, containerReturn)
        }
    )
>>>>>>> backup/1.21.1
}

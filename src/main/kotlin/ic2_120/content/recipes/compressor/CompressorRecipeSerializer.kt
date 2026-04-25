package ic2_120.content.recipes.compressor

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
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter { it.output }
        ).apply(instance) { ingredient, inputCount, output ->
            CompressorRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, output)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, CompressorRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            buf.writeVarInt(recipe.inputCount)
            ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val inputCount = buf.readVarInt()
            val output = ItemStack.PACKET_CODEC.decode(buf)
            CompressorRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, output)
        }
    )
}

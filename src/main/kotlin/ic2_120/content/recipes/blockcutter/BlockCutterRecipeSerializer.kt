package ic2_120.content.recipes.blockcutter

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

@ModMachineRecipe(id = "cutting", recipeClass = BlockCutterRecipe::class)
object BlockCutterRecipeSerializer : RecipeSerializer<BlockCutterRecipe> {
    override fun codec(): MapCodec<BlockCutterRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            Codec.INT.fieldOf("input_count").orElse(1).forGetter { it.inputCount },
            Codec.FLOAT.fieldOf("material_hardness").forGetter { it.materialHardness },
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter { it.output }
        ).apply(instance) { ingredient, inputCount, materialHardness, output ->
            BlockCutterRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, materialHardness, output)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, BlockCutterRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            buf.writeVarInt(recipe.inputCount)
            buf.writeFloat(recipe.materialHardness)
            ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val inputCount = buf.readVarInt()
            val materialHardness = buf.readFloat()
            val output = ItemStack.PACKET_CODEC.decode(buf)
            BlockCutterRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, materialHardness, output)
        }
    )
}

package ic2_120.content.recipes.centrifuge

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

@ModMachineRecipe(id = "centrifuging", recipeClass = CentrifugeRecipe::class)
object CentrifugeRecipeSerializer : RecipeSerializer<CentrifugeRecipe> {
    override fun codec(): MapCodec<CentrifugeRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            Codec.INT.fieldOf("input_count").orElse(1).forGetter { it.inputCount },
            Codec.INT.fieldOf("min_heat").forGetter { it.minHeat },
            ItemStack.VALIDATED_CODEC.listOf().fieldOf("results").forGetter { it.outputs }
        ).apply(instance) { ingredient, inputCount, minHeat, outputs ->
            CentrifugeRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, minHeat, outputs)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, CentrifugeRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            buf.writeVarInt(recipe.inputCount)
            buf.writeVarInt(recipe.minHeat)
            buf.writeVarInt(recipe.outputs.size)
            recipe.outputs.forEach { output ->
                ItemStack.PACKET_CODEC.encode(buf, output.copy())
            }
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val inputCount = buf.readVarInt()
            val minHeat = buf.readVarInt()
            val outputCount = buf.readVarInt()
            val outputs = mutableListOf<ItemStack>()
            repeat(outputCount) {
                outputs.add(ItemStack.PACKET_CODEC.decode(buf))
            }
            CentrifugeRecipe(Identifier.of("ic2_120", "_"), ingredient, inputCount, minHeat, outputs)
        }
    )
}

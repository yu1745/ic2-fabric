package ic2_120.content.recipes.macerator

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
}

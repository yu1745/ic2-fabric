package ic2_120.content.recipes.extractor

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
}

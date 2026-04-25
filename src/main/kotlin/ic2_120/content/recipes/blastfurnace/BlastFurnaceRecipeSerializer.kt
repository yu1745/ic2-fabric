package ic2_120.content.recipes.blastfurnace

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.util.Identifier

@ModMachineRecipe(id = "blast_furnacing", recipeClass = BlastFurnaceRecipe::class)
object BlastFurnaceRecipeSerializer : RecipeSerializer<BlastFurnaceRecipe> {
    override fun codec(): MapCodec<BlastFurnaceRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            ItemStack.VALIDATED_CODEC.fieldOf("steel_output").forGetter { it.steelOutput },
            ItemStack.VALIDATED_CODEC.fieldOf("slag_output").forGetter { it.slagOutput }
        ).apply(instance) { ingredient, steelOutput, slagOutput ->
            BlastFurnaceRecipe(Identifier.of("ic2_120", "_"), ingredient, steelOutput, slagOutput)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, BlastFurnaceRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            ItemStack.PACKET_CODEC.encode(buf, recipe.steelOutput.copy())
            ItemStack.PACKET_CODEC.encode(buf, recipe.slagOutput.copy())
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val steelOutput = ItemStack.PACKET_CODEC.decode(buf)
            val slagOutput = ItemStack.PACKET_CODEC.decode(buf)
            BlastFurnaceRecipe(Identifier.of("ic2_120", "_"), ingredient, steelOutput, slagOutput)
        }
    )
}

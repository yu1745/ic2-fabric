package ic2_120.content.recipes.metalformer

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

@ModMachineRecipe(id = "metal_forming", recipeClass = MetalFormerRecipe::class)
object MetalFormerRecipeSerializer : RecipeSerializer<MetalFormerRecipe> {
    override fun codec(): MapCodec<MetalFormerRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter { it.output },
            Codec.STRING.fieldOf("mode").orElse("rolling").forGetter { recipe ->
                when (recipe) {
                    is CuttingRecipe -> "cutting"
                    is ExtrudingRecipe -> "extruding"
                    else -> "rolling"
                }
            }
        ).apply(instance) { ingredient, output, mode ->
            when (mode.lowercase()) {
                "cutting" -> CuttingRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
                "extruding" -> ExtrudingRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
                else -> RollingRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
            }
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, MetalFormerRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
            val mode = when (recipe) {
                is CuttingRecipe -> "cutting"
                is ExtrudingRecipe -> "extruding"
                else -> "rolling"
            }
            buf.writeString(mode)
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val output = ItemStack.PACKET_CODEC.decode(buf)
            val mode = buf.readString()
            when (mode) {
                "cutting" -> CuttingRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
                "extruding" -> ExtrudingRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
                else -> RollingRecipe(Identifier.of("ic2_120", "_"), ingredient, output)
            }
        }
    )
}

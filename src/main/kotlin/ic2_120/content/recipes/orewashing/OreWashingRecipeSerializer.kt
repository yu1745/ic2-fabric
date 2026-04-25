package ic2_120.content.recipes.orewashing

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

@ModMachineRecipe(id = "ore_washing", recipeClass = OreWashingRecipe::class)
object OreWashingRecipeSerializer : RecipeSerializer<OreWashingRecipe> {
    override fun codec(): MapCodec<OreWashingRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter { it.ingredient },
            ItemStack.VALIDATED_CODEC.listOf().fieldOf("outputs").forGetter { it.outputItems },
            Codec.LONG.fieldOf("water_consumption_mb").orElse(1000L).forGetter { it.waterConsumptionMb }
        ).apply(instance) { ingredient, outputs, waterConsumption ->
            OreWashingRecipe(Identifier.of("ic2_120", "_"), ingredient, outputs, waterConsumption)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, OreWashingRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.ingredient)
            buf.writeVarInt(recipe.outputItems.size)
            recipe.outputItems.forEach { output ->
                ItemStack.PACKET_CODEC.encode(buf, output.copy())
            }
            buf.writeLong(recipe.waterConsumptionMb)
        },
        { buf ->
            val ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val outputCount = buf.readVarInt()
            val outputs = (0 until outputCount).map {
                ItemStack.PACKET_CODEC.decode(buf)
            }
            val waterConsumption = buf.readLong()
            OreWashingRecipe(Identifier.of("ic2_120", "_"), ingredient, outputs, waterConsumption)
        }
    )
}

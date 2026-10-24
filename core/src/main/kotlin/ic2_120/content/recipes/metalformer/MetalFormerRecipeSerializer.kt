package ic2_120.content.recipes.metalformer

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

/**
 * 金属成型机配方序列化器（三种模式共用）
 */
@ModMachineRecipe(id = "metal_forming", recipeClass = MetalFormerRecipe::class)
object MetalFormerRecipeSerializer : RecipeSerializer<MetalFormerRecipe> {
    override fun read(id: Identifier, json: JsonObject): MetalFormerRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))

        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        val output = ItemStack(item, count)

        val mode = JsonHelper.getString(json, "mode", "rolling")

        return when (mode?.lowercase()) {
            "cutting" -> CuttingRecipe(id, ingredient, output)
            "extruding" -> ExtrudingRecipe(id, ingredient, output)
            else -> RollingRecipe(id, ingredient, output)  // 默认是辊压模式
        }
    }

    override fun read(id: Identifier, buf: PacketByteBuf): MetalFormerRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        val mode = buf.readString()

        return when (mode) {
            "cutting" -> CuttingRecipe(id, ingredient, output)
            "extruding" -> ExtrudingRecipe(id, ingredient, output)
            else -> RollingRecipe(id, ingredient, output)
        }
    }

    override fun write(buf: PacketByteBuf, recipe: MetalFormerRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())

        val mode = when (recipe) {
            is CuttingRecipe -> "cutting"
            is ExtrudingRecipe -> "extruding"
            else -> "rolling"
        }
        buf.writeString(mode)
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
>>>>>>> backup/1.21.1
}

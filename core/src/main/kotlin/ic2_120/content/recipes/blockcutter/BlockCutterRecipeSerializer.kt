package ic2_120.content.recipes.blockcutter

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
 * 方块切割机配方序列化器
 *
 * JSON 格式：
 * {
 *   "type": "ic2_120:cutting",
 *   "ingredient": { "item": "minecraft:oak_planks" },
 *   "input_count": 1,
 *   "material_hardness": 2.0,
 *   "result": { "item": "minecraft:oak_slab", "count": 9 }
 * }
 */
@ModMachineRecipe(id = "cutting", recipeClass = BlockCutterRecipe::class)
object BlockCutterRecipeSerializer : RecipeSerializer<BlockCutterRecipe> {
    override fun read(id: Identifier, json: JsonObject): BlockCutterRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))
        val inputCount = JsonHelper.getInt(json, "input_count", 1)
        val materialHardness = JsonHelper.getFloat(json, "material_hardness")
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)

        return BlockCutterRecipe(id, ingredient, inputCount, materialHardness, ItemStack(item, count))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): BlockCutterRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val inputCount = buf.readVarInt()
        val materialHardness = buf.readFloat()
        val output = buf.readItemStack()

        return BlockCutterRecipe(id, ingredient, inputCount, materialHardness, output)
    }

    override fun write(buf: PacketByteBuf, recipe: BlockCutterRecipe) {
        recipe.ingredient.write(buf)
        buf.writeVarInt(recipe.inputCount)
        buf.writeFloat(recipe.materialHardness)
        buf.writeItemStack(recipe.output.copy())
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
>>>>>>> backup/1.21.1
}

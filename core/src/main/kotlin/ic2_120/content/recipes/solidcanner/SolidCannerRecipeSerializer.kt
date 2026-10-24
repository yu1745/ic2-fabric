package ic2_120.content.recipes.solidcanner

<<<<<<< HEAD
import ic2_120.registry.annotation.ModMachineRecipe
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper
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
>>>>>>> backup/1.21.1

@ModMachineRecipe(id = "solid_canning", recipeClass = SolidCannerRecipe::class)
object SolidCannerRecipeSerializer : RecipeSerializer<SolidCannerRecipe> {

<<<<<<< HEAD
    override fun read(id: Identifier, json: JsonObject): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "slot0_ingredient"))
        val slot0Count = JsonHelper.getInt(json, "slot0_count")
        val slot1Ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "slot1_ingredient"))
        val slot1Count = JsonHelper.getInt(json, "slot1_count")
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = net.minecraft.registry.Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, ItemStack(item, count))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): SolidCannerRecipe {
        val slot0Ingredient = Ingredient.fromPacket(buf)
        val slot0Count = buf.readInt()
        val slot1Ingredient = Ingredient.fromPacket(buf)
        val slot1Count = buf.readInt()
        val output = buf.readItemStack()
        return SolidCannerRecipe(id, slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, output)
    }

    override fun write(buf: PacketByteBuf, recipe: SolidCannerRecipe) {
        recipe.slot0Ingredient.write(buf)
        buf.writeInt(recipe.slot0Count)
        recipe.slot1Ingredient.write(buf)
        buf.writeInt(recipe.slot1Count)
        buf.writeItemStack(recipe.output.copy())
    }
=======
    override fun codec(): MapCodec<SolidCannerRecipe> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("slot0_ingredient").forGetter { it.slot0Ingredient },
            Codec.INT.fieldOf("slot0_count").forGetter { it.slot0Count },
            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("slot1_ingredient").forGetter { it.slot1Ingredient },
            Codec.INT.fieldOf("slot1_count").forGetter { it.slot1Count },
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter { it.output }
        ).apply(instance) { slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, result ->
            SolidCannerRecipe(Identifier.of("ic2_120", "_"), slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, result)
        }
    }

    override fun packetCodec(): PacketCodec<RegistryByteBuf, SolidCannerRecipe> = PacketCodec.ofStatic(
        { buf, recipe ->
            Ingredient.PACKET_CODEC.encode(buf, recipe.slot0Ingredient)
            buf.writeInt(recipe.slot0Count)
            Ingredient.PACKET_CODEC.encode(buf, recipe.slot1Ingredient)
            buf.writeInt(recipe.slot1Count)
            ItemStack.PACKET_CODEC.encode(buf, recipe.output.copy())
        },
        { buf ->
            val slot0Ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val slot0Count = buf.readInt()
            val slot1Ingredient = Ingredient.PACKET_CODEC.decode(buf)
            val slot1Count = buf.readInt()
            val output = ItemStack.PACKET_CODEC.decode(buf)
            SolidCannerRecipe(Identifier.of("ic2_120", "_"), slot0Ingredient, slot0Count, slot1Ingredient, slot1Count, output)
        }
    )
>>>>>>> backup/1.21.1
}

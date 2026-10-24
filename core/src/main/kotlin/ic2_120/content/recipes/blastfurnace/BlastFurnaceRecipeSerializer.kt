package ic2_120.content.recipes.blastfurnace

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
 * 高炉配方序列化器
 *
 * JSON 格式：
 * {
 *   "type": "ic2_120:blast_furnacing",
 *   "ingredient": { "item": "minecraft:iron_ingot" },
 *   "steel_output": { "item": "ic2_120:steel_ingot", "count": 1 },
 *   "slag_output": { "item": "ic2_120:slag", "count": 1 }
 * }
 */
@ModMachineRecipe(id = "blast_furnacing", recipeClass = BlastFurnaceRecipe::class)
object BlastFurnaceRecipeSerializer : RecipeSerializer<BlastFurnaceRecipe> {
    override fun read(id: Identifier, json: JsonObject): BlastFurnaceRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))

        val steelOutputJson = JsonHelper.getObject(json, "steel_output")
        val steelItemId = Identifier(JsonHelper.getString(steelOutputJson, "item"))
        val steelItem = Registries.ITEM.get(steelItemId)
        val steelCount = JsonHelper.getInt(steelOutputJson, "count", 1)
        val steelOutput = ItemStack(steelItem, steelCount)

        val slagOutputJson = JsonHelper.getObject(json, "slag_output")
        val slagItemId = Identifier(JsonHelper.getString(slagOutputJson, "item"))
        val slagItem = Registries.ITEM.get(slagItemId)
        val slagCount = JsonHelper.getInt(slagOutputJson, "count", 1)
        val slagOutput = ItemStack(slagItem, slagCount)

        return BlastFurnaceRecipe(id, ingredient, steelOutput, slagOutput)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): BlastFurnaceRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val steelOutput = buf.readItemStack()
        val slagOutput = buf.readItemStack()

        return BlastFurnaceRecipe(id, ingredient, steelOutput, slagOutput)
    }

    override fun write(buf: PacketByteBuf, recipe: BlastFurnaceRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.steelOutput.copy())
        buf.writeItemStack(recipe.slagOutput.copy())
    }
=======
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
>>>>>>> backup/1.21.1
}

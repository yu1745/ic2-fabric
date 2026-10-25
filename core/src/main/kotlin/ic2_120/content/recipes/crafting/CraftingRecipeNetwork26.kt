package ic2_120.content.recipes.crafting

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

internal object CraftingRecipeNetwork26 {
    fun writeShaped(buf: RegistryFriendlyByteBuf, rows: List<String>, keys: Map<Char, Ingredient>, result: ItemStack) {
        buf.writeVarInt(rows.size)
        rows.forEach(buf::writeUtf)
        buf.writeVarInt(keys.size)
        for ((key, ingredient) in keys) {
            buf.writeChar(key.code)
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient)
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, result)
    }

    fun readShaped(buf: RegistryFriendlyByteBuf): Triple<List<String>, Map<Char, Ingredient>, ItemStack> {
        val rows = List(buf.readVarInt()) { buf.readUtf() }
        val keys = linkedMapOf<Char, Ingredient>()
        repeat(buf.readVarInt()) { keys[buf.readChar()] = Ingredient.CONTENTS_STREAM_CODEC.decode(buf) }
        return Triple(rows, keys, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf))
    }

    fun writeShapeless(buf: RegistryFriendlyByteBuf, ingredients: List<Ingredient>, result: ItemStack) {
        buf.writeVarInt(ingredients.size)
        ingredients.forEach { Ingredient.CONTENTS_STREAM_CODEC.encode(buf, it) }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, result)
    }

    fun readShapeless(buf: RegistryFriendlyByteBuf): Pair<List<Ingredient>, ItemStack> {
        val ingredients = List(buf.readVarInt()) { Ingredient.CONTENTS_STREAM_CODEC.decode(buf) }
        return ingredients to ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
    }
}

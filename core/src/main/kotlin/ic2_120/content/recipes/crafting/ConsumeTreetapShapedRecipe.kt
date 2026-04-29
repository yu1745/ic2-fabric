package ic2_120.content.recipes.crafting

import com.mojang.serialization.MapCodec
import ic2_120.content.item.Treetap
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RawShapedRecipe
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.util.Optional
import java.util.stream.Stream

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())

/**
 * 工作台有序配方：消耗参与合成的木龙头，而不是返回损耗后的余物。
 */
class ConsumeTreetapShapedRecipe(delegate: ShapedRecipe, raw: RawShapedRecipe = RawShapedRecipe(delegate.width, delegate.height, delegate.ingredients, Optional.empty())) : ShapedRecipe(
    delegate.group,
    delegate.category,
    raw,
    delegate.getResult(EMPTY_LOOKUP),
    delegate.showNotification()
) {
    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
        val remainder = DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)
        for (slot in 0 until input.getSize()) {
            val stack = input.getStackInSlot(slot)
            if (stack.isEmpty) continue
            remainder[slot] = if (stack.item is Treetap) ItemStack.EMPTY
            else stack.item.recipeRemainder?.let { ItemStack(it) } ?: ItemStack.EMPTY
        }
        return remainder
    }

    override fun getSerializer(): RecipeSerializer<*> = ConsumeTreetapShapedRecipeSerializer
}

object ConsumeTreetapShapedRecipeSerializer : RecipeSerializer<ConsumeTreetapShapedRecipe> {
    override fun codec(): MapCodec<ConsumeTreetapShapedRecipe> =
        ShapedRecipe.Serializer.CODEC.xmap(
            { ConsumeTreetapShapedRecipe(it) },
            { it }
        )

    override fun packetCodec(): PacketCodec<RegistryByteBuf, ConsumeTreetapShapedRecipe> =
        ShapedRecipe.Serializer.PACKET_CODEC.xmap(
            { ConsumeTreetapShapedRecipe(it) },
            { it }
        )
}

object ConsumeTreetapShapedRecipeDatagen {
    fun offer(
        exporter: RecipeExporter,
        recipeId: Identifier,
        result: Item,
        pattern: List<String>,
        keys: Map<Char, Item>,
        count: Int = 1,
        category: String = "misc"
    ) {
        val ingredientMap = keys.mapValues { (_, item) -> Ingredient.ofItems(item) }
        val raw = RawShapedRecipe.create(ingredientMap, pattern)
        val resultStack = ItemStack(result, count)
        val shaped = ShapedRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), raw, resultStack)
        val recipe = ConsumeTreetapShapedRecipe(shaped, raw)
        exporter.accept(recipeId, recipe, null)
    }
}

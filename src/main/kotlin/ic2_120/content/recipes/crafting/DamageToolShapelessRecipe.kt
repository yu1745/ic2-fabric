package ic2_120.content.recipes.crafting

import com.mojang.serialization.MapCodec
import ic2_120.content.item.Cutter
import ic2_120.content.item.ForgeHammer
import ic2_120.content.item.Treetap
import ic2_120.content.item.Wrench
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.util.stream.Stream

private val EMPTY_LOOKUP = RegistryWrapper.WrapperLookup.of(Stream.empty())

/**
 * 工作台无序配方：合成时对 IC2 工具（锻造锤、切割机、木龙头、扳手）返回耐久-1 的本体，
 * 其他材料正常消耗不返回余物。
 *
 * 与 Item.getRecipeRemainder() 不同，该配方类型只在明确的合成路径中返回损伤工具，
 * 而不会在原版合并修复路径触发余物返回，从而避免无限刷物品漏洞。
 */
class DamageToolShapelessRecipe(delegate: ShapelessRecipe) : ShapelessRecipe(
    delegate.group,
    delegate.category,
    delegate.getResult(EMPTY_LOOKUP),
    delegate.ingredients
) {
    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
        val remainder = DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)
        for (slot in 0 until input.getSize()) {
            val stack = input.getStackInSlot(slot)
            if (stack.isEmpty) continue
            val item = stack.item
            if (isDamageableCraftingTool(item)) {
                val result = stack.copy()
                if (result.damage < result.maxDamage - 1) {
                    result.damage += 1
                    remainder[slot] = result
                }
            } else {
                remainder[slot] = item.recipeRemainder?.let { ItemStack(it) } ?: ItemStack.EMPTY
            }
        }
        return remainder
    }

    override fun getSerializer(): RecipeSerializer<*> = DamageToolShapelessRecipeSerializer

    companion object {
        fun isDamageableCraftingTool(item: Item): Boolean =
            item is ForgeHammer || item is Cutter || item is Treetap || item is Wrench
    }
}

object DamageToolShapelessRecipeSerializer : RecipeSerializer<DamageToolShapelessRecipe> {
    override fun codec(): MapCodec<DamageToolShapelessRecipe> =
        RecipeSerializer.SHAPELESS.codec().xmap(
            { DamageToolShapelessRecipe(it) },
            { it }
        )

    override fun packetCodec(): PacketCodec<RegistryByteBuf, DamageToolShapelessRecipe> =
        RecipeSerializer.SHAPELESS.packetCodec().xmap(
            { DamageToolShapelessRecipe(it) },
            { it }
        )
}

object DamageToolShapelessRecipeDatagen {
    fun offer(
        exporter: RecipeExporter,
        recipeId: Identifier,
        result: Item,
        resultCount: Int = 1,
        ingredients: List<Ingredient>,
        category: String = "misc"
    ) {
        val defaultedIngredients = DefaultedList.ofSize<Ingredient>(ingredients.size, Ingredient.EMPTY)
        for (i in ingredients.indices) defaultedIngredients[i] = ingredients[i]
        val shapeless = ShapelessRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), ItemStack(result, resultCount), defaultedIngredients)
        val recipe = DamageToolShapelessRecipe(shapeless)
        exporter.accept(recipeId, recipe, null)
    }

    fun toolIngredient(tool: Item): Ingredient =
        Ingredient.ofItems(tool)
}

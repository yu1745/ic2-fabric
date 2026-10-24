package ic2_120.content.recipes.crafting

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.item.Cutter
import ic2_120.content.item.ForgeHammer
import ic2_120.content.item.Treetap
import ic2_120.content.item.Wrench
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.NonNullList
import java.util.function.Consumer

/**
 * 工作台无序配方：合成时对 IC2 工具（锻造锤、切割机、木龙头、扳手）返回耐久-1 的本体，
 * 其他材料正常消耗不返回余物。
 *
 * 与 Item.getRecipeRemainder() 不同，该配方类型只在明确的合成路径中返回损伤工具，
 * 而不会在原版合并修复路径触发余物返回，从而避免无限刷物品漏洞。
 */
class DamageToolShapelessRecipe(delegate: ShapelessRecipe) : ShapelessRecipe(
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.getOutput(RegistryAccess.EMPTY),
    delegate.ingredients
) {
    override fun getRemainder(inventory: CraftingContainer): NonNullList<ItemStack> {
        val remainder = NonNullList.withSize(inventory.size(), ItemStack.EMPTY)
        for (slot in 0 until inventory.size()) {
            val stack = inventory.getStack(slot)
            if (stack.isEmpty) continue
            val item = stack.item
            if (isDamageableCraftingTool(item)) {
                val result = stack.copy()
                if (result.damage < result.maxDamage - 1) {
                    result.damage += 1
                    remainder[slot] = result
                }
                // 已达最大耐久损耗时不返回（工具损坏）
            } else {
                remainder[slot] = item.getRecipeRemainder(stack)
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

@ModMachineRecipe(id = "damage_tool_shapeless", recipeClass = DamageToolShapelessRecipe::class)
object DamageToolShapelessRecipeSerializer : RecipeSerializer<DamageToolShapelessRecipe> {
    override fun fromJson(id: ResourceLocation, json: JsonObject, lookup: HolderLookup.Provider): DamageToolShapelessRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, json)
        return DamageToolShapelessRecipe(shapeless)
    }

    override fun read(id: ResourceLocation, buf: FriendlyByteBuf): DamageToolShapelessRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, buf)
        return DamageToolShapelessRecipe(shapeless)
    }

    override fun write(buf: FriendlyByteBuf, recipe: DamageToolShapelessRecipe) {
        RecipeSerializer.SHAPELESS.write(buf, recipe)
    }
}

/**
 * 生成使用 damage_tool_shapeless 配方类型的无序合成配方 JSON。
 *
 * 用法示例（Kotlin datagen）：
 * ```
 * DamageToolShapelessRecipeDatagen.offer(
 *     exporter = exporter,
 *     recipeId = BronzePlate::class.id(),
 *     result = BronzePlate::class.instance(),
 *     resultCount = 1,
 *     ingredients = listOf(
 *         DamageToolShapelessRecipeDatagen.toolIngredient<ForgeHammer>(),
 *         Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
 *     ),
 *     category = "misc"
 * )
 * ```
 */
object DamageToolShapelessRecipeDatagen {
    fun offer(
        exporter: Consumer<RecipeOutput>,
        recipeId: ResourceLocation,
        result: Item,
        resultCount: Int = 1,
        ingredients: List<Ingredient>,
        category: String = "misc"
    ) {
        exporter.accept(
            Provider(
                recipeId = recipeId,
                result = result,
                resultCount = resultCount,
                ingredients = ingredients,
                category = category
            )
        )
    }

    fun toolIngredient(tool: Item): Ingredient =
        Ingredient.ofItems(tool)

    private class Provider(
        private val recipeId: ResourceLocation,
        private val result: Item,
        private val resultCount: Int,
        private val ingredients: List<Ingredient>,
        private val category: String
    ) : RecipeOutput {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "ic2_120:damage_tool_shapeless")
            json.addProperty("category", category)

            val ingredientsArr = JsonArray()
            for (ingredient in ingredients) {
                ingredientsArr.add(ingredient.toJson())
            }
            json.add("ingredients", ingredientsArr)

            val resultObj = JsonObject()
            resultObj.addProperty("item", BuiltInRegistries.ITEM.getId(result).toString())
            if (resultCount > 1) resultObj.addProperty("count", resultCount)
            json.add("result", resultObj)
        }

        override fun getSerializer(): RecipeSerializer<*> = DamageToolShapelessRecipeSerializer

        override fun getRecipeId(): ResourceLocation = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): ResourceLocation? = null
    }
}

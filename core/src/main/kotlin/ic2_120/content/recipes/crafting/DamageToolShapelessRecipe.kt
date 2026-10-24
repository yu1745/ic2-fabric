package ic2_120.content.recipes.crafting

<<<<<<< HEAD
import com.google.gson.JsonArray
import com.google.gson.JsonObject
=======
import com.mojang.serialization.MapCodec
>>>>>>> backup/1.21.1
import ic2_120.content.item.Cutter
import ic2_120.content.item.ForgeHammer
import ic2_120.content.item.Treetap
import ic2_120.content.item.Wrench
<<<<<<< HEAD
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.util.function.Consumer
=======
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
>>>>>>> backup/1.21.1

/**
 * 工作台无序配方：合成时对 IC2 工具（锻造锤、切割机、木龙头、扳手）返回耐久-1 的本体，
 * 其他材料正常消耗不返回余物。
 *
 * 与 Item.getRecipeRemainder() 不同，该配方类型只在明确的合成路径中返回损伤工具，
 * 而不会在原版合并修复路径触发余物返回，从而避免无限刷物品漏洞。
 */
class DamageToolShapelessRecipe(delegate: ShapelessRecipe) : ShapelessRecipe(
<<<<<<< HEAD
    delegate.id,
    delegate.group,
    delegate.category,
    delegate.getOutput(DynamicRegistryManager.EMPTY),
    delegate.ingredients
) {
    override fun getRemainder(inventory: RecipeInputInventory): DefaultedList<ItemStack> {
        val remainder = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY)
        for (slot in 0 until inventory.size()) {
            val stack = inventory.getStack(slot)
=======
    delegate.group,
    delegate.category,
    delegate.getResult(EMPTY_LOOKUP),
    delegate.ingredients
) {
    override fun getRemainder(input: CraftingRecipeInput): DefaultedList<ItemStack> {
        val remainder = DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY)
        for (slot in 0 until input.getSize()) {
            val stack = input.getStackInSlot(slot)
>>>>>>> backup/1.21.1
            if (stack.isEmpty) continue
            val item = stack.item
            if (isDamageableCraftingTool(item)) {
                val result = stack.copy()
                if (result.damage < result.maxDamage - 1) {
                    result.damage += 1
                    remainder[slot] = result
                }
<<<<<<< HEAD
                // 已达最大耐久损耗时不返回（工具损坏）
            } else {
                remainder[slot] = item.getRecipeRemainder(stack)
=======
            } else {
                remainder[slot] = item.recipeRemainder?.let { ItemStack(it) } ?: ItemStack.EMPTY
>>>>>>> backup/1.21.1
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

<<<<<<< HEAD
@ModMachineRecipe(id = "damage_tool_shapeless", recipeClass = DamageToolShapelessRecipe::class)
object DamageToolShapelessRecipeSerializer : RecipeSerializer<DamageToolShapelessRecipe> {
    override fun read(id: Identifier, json: JsonObject): DamageToolShapelessRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, json)
        return DamageToolShapelessRecipe(shapeless)
    }

    override fun read(id: Identifier, buf: PacketByteBuf): DamageToolShapelessRecipe {
        val shapeless = RecipeSerializer.SHAPELESS.read(id, buf)
        return DamageToolShapelessRecipe(shapeless)
    }

    override fun write(buf: PacketByteBuf, recipe: DamageToolShapelessRecipe) {
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
 *         Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
 *     ),
 *     category = "misc"
 * )
 * ```
 */
object DamageToolShapelessRecipeDatagen {
    fun offer(
        exporter: Consumer<RecipeJsonProvider>,
=======
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
>>>>>>> backup/1.21.1
        recipeId: Identifier,
        result: Item,
        resultCount: Int = 1,
        ingredients: List<Ingredient>,
        category: String = "misc"
    ) {
<<<<<<< HEAD
        exporter.accept(
            Provider(
                recipeId = recipeId,
                result = result,
                resultCount = resultCount,
                ingredients = ingredients,
                category = category
            )
        )
=======
        val defaultedIngredients = DefaultedList.ofSize<Ingredient>(ingredients.size, Ingredient.EMPTY)
        for (i in ingredients.indices) defaultedIngredients[i] = ingredients[i]
        val shapeless = ShapelessRecipe("", CraftingRecipeCategory.valueOf(category.uppercase()), ItemStack(result, resultCount), defaultedIngredients)
        val recipe = DamageToolShapelessRecipe(shapeless)
        exporter.accept(recipeId, recipe, null)
>>>>>>> backup/1.21.1
    }

    fun toolIngredient(tool: Item): Ingredient =
        Ingredient.ofItems(tool)
<<<<<<< HEAD

    private class Provider(
        private val recipeId: Identifier,
        private val result: Item,
        private val resultCount: Int,
        private val ingredients: List<Ingredient>,
        private val category: String
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "ic2_120:damage_tool_shapeless")
            json.addProperty("category", category)

            val ingredientsArr = JsonArray()
            for (ingredient in ingredients) {
                ingredientsArr.add(ingredient.toJson())
            }
            json.add("ingredients", ingredientsArr)

            val resultObj = JsonObject()
            resultObj.addProperty("item", Registries.ITEM.getId(result).toString())
            if (resultCount > 1) resultObj.addProperty("count", resultCount)
            json.add("result", resultObj)
        }

        override fun getSerializer(): RecipeSerializer<*> = DamageToolShapelessRecipeSerializer

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
=======
>>>>>>> backup/1.21.1
}

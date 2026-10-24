package ic2_120.content.recipes.crafting

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.serialization.MapCodec
import ic2_120.content.item.Cutter
import ic2_120.content.item.ForgeHammer
import ic2_120.content.item.Treetap
import ic2_120.content.item.Wrench
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapelessRecipe

/**
 * 工作台无序配方：合成时对 IC2 工具（锻造锤、切割机、木龙头、扳手）返回耐久-1 的本体，
 * 其他材料正常消耗不返回余物。
 *
 * 与 Item.getRecipeRemainder() 不同，该配方类型只在明确的合成路径中返回损伤工具，
 * 而不会在原版合并修复路径触发余物返回，从而避免无限刷物品漏洞。
 */
class DamageToolShapelessRecipe(
    internal val recipeIngredients: List<Ingredient>,
    result: ItemStack,
    group: String = "",
) : ShapelessRecipe(group, CraftingBookCategory.MISC, result, NonNullList.of(Ingredient.EMPTY, *recipeIngredients.toTypedArray())) {
    override fun getRemainingItems(input: CraftingInput): NonNullList<ItemStack> {
        val remainder = NonNullList.withSize(input.size(), ItemStack.EMPTY)
        for (slot in 0 until input.size()) {
            val stack = input.getItem(slot)
            if (stack.isEmpty) continue
            val item = stack.item
            if (isDamageableCraftingTool(item)) {
                val result = stack.copy()
                if (result.damageValue < result.maxDamage - 1) {
                    result.damageValue += 1
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

fun decodeDamageToolShapelessJson(id: ResourceLocation, json: JsonObject): DamageToolShapelessRecipe {
    val ingredients = json.getAsJsonArray("ingredients").map { INGREDIENT_CODEC_FROM(it) }
    val resultObj = json.getAsJsonObject("result")
    val resultItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resultObj.get("item").asString))
    val count = if (resultObj.has("count")) resultObj.get("count").asInt else 1
    return DamageToolShapelessRecipe(ingredients, ItemStack(resultItem, count))
}

private fun INGREDIENT_CODEC_FROM(element: com.google.gson.JsonElement): Ingredient =
    ic2_120.content.recipes.INGREDIENT_CODEC(element.asJsonObject)

fun encodeDamageToolShapelessJson(json: JsonObject, recipe: DamageToolShapelessRecipe) {
    json.addProperty("type", "ic2_120:damage_tool_shapeless")

    val ingredientsArr = JsonArray()
    for (ingredient in recipe.recipeIngredients) {
        val obj = JsonObject()
        val first = ingredient.items.firstOrNull()
        obj.addProperty("item", first?.let { BuiltInRegistries.ITEM.getKey(it.item)?.toString() } ?: "minecraft:air")
        ingredientsArr.add(obj)
    }
    json.add("ingredients", ingredientsArr)

    val result = recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY)
    val resultObj = JsonObject()
    BuiltInRegistries.ITEM.getKey(result.item)?.let { resultObj.addProperty("item", it.toString()) }
    if (result.count > 1) resultObj.addProperty("count", result.count)
    json.add("result", resultObj)
}

@ModMachineRecipe(id = "damage_tool_shapeless", recipeClass = DamageToolShapelessRecipe::class)
object DamageToolShapelessRecipeSerializer : RecipeSerializer<DamageToolShapelessRecipe> {
    override fun codec(): MapCodec<DamageToolShapelessRecipe> = ic2_120.content.recipes.RecipeCodecBridge.jsonMapCodec(
        "ic2_120:damage_tool_shapeless",
        ::decodeDamageToolShapelessJson,
        ::encodeDamageToolShapelessJson
    )

    // TODO(1.21.1): 配方不走网络同步（双端各自读 datapack），流编解码仅做占位实现
    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, DamageToolShapelessRecipe> {
        val delegate = RecipeSerializer.SHAPELESS_RECIPE.streamCodec()
        return StreamCodec.ofMember(
            { recipe: DamageToolShapelessRecipe, buf: RegistryFriendlyByteBuf -> delegate.encode(buf, recipe) },
            { _ ->
                DamageToolShapelessRecipe(listOf(Ingredient.EMPTY), ItemStack.EMPTY)
            }
        )
    }
}

/**
 * 生成使用 damage_tool_shapeless 配方类型的无序合成配方 JSON。
 */
object DamageToolShapelessRecipeDatagen {
    fun offer(
        exporter: net.minecraft.data.recipes.RecipeOutput,
        recipeId: ResourceLocation,
        result: Item,
        resultCount: Int = 1,
        ingredients: List<Ingredient>,
        category: String = "misc"
    ) {
        val recipe = DamageToolShapelessRecipe(ingredients, ItemStack(result, resultCount))
        exporter.accept(recipeId, recipe, null)
    }

    fun toolIngredient(tool: Item): Ingredient =
        Ingredient.of(tool)
}

package ic2_120.content.recipes.crafting

import ic2_120.compat.accept

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.serialization.MapCodec
import ic2_120.content.recipes.INGREDIENT_CODEC
import ic2_120.content.item.Treetap
import ic2_120.registry.annotation.ModMachineRecipe
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapedRecipePattern

/**
 * 工作台有序配方：消耗参与合成的扳手/电动工具，完全不返回余物，并继承电量累加功能。
 */
class ConsumeTreetapShapedRecipe(
    internal val patternRows: List<String>,
    internal val patternKeys: Map<Char, Ingredient>,
    result: ItemStack,
    group: String = "",
) : ShapedRecipe(net.minecraft.world.item.crafting.Recipe.CommonInfo(true), net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, group), ShapedRecipePattern.of(patternKeys, patternRows), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result)) {
    internal val recipeResult: ItemStack = result.copy()

    override fun getRemainingItems(input: CraftingInput): NonNullList<ItemStack> {
        // 消耗参与合成的木龙头，其余材料按原版余物规则返回
        val remainder = NonNullList.withSize(input.size(), ItemStack.EMPTY)
        for (slot in 0 until input.size()) {
            val stack = input.getItem(slot)
            if (stack.isEmpty) continue
            remainder[slot] = if (stack.item is Treetap) ItemStack.EMPTY else stack.item.craftingRemainder?.create() ?: ItemStack.EMPTY
        }
        return remainder
    }

    @Suppress("UNCHECKED_CAST")
    override fun getSerializer(): RecipeSerializer<ShapedRecipe> = ConsumeTreetapShapedRecipeSerializer.serializer as RecipeSerializer<ShapedRecipe>
}

fun decodeConsumeTreetapShapedJson(id: Identifier, json: JsonObject): ConsumeTreetapShapedRecipe {
    val keyMap = linkedMapOf<Char, Ingredient>()
    val keyObj = json.getAsJsonObject("key")
    for ((charKey, value) in keyObj.entrySet()) {
        keyMap[charKey[0]] = INGREDIENT_CODEC(value.asJsonObject)
    }
    val rows = json.getAsJsonArray("pattern").map { it.asString }
    val resultObj = json.getAsJsonObject("result")
    val resultItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(resultObj.get("item").asString))
    val count = if (resultObj.has("count")) resultObj.get("count").asInt else 1
    return ConsumeTreetapShapedRecipe(rows, keyMap, ItemStack(resultItem, count))
}

fun encodeConsumeTreetapShapedJson(json: JsonObject, recipe: ConsumeTreetapShapedRecipe) {
    json.addProperty("type", "ic2_120:consume_treetap_shaped")

    val keyObj = JsonObject()
    for ((charKey, ingredient) in recipe.patternKeys) {
        keyObj.add(charKey.toString(), ic2_120.content.recipes.ingredientJson26(ingredient))
    }
    json.add("key", keyObj)

    val patternArr = JsonArray()
    for (row in recipe.patternRows) patternArr.add(row)
    json.add("pattern", patternArr)

    val result = recipe.recipeResult.copy()
    val resultObj = JsonObject()
    BuiltInRegistries.ITEM.getKey(result.item)?.let { resultObj.addProperty("item", it.toString()) }
    if (result.count > 1) resultObj.addProperty("count", result.count)
    json.add("result", resultObj)
    json.addProperty("show_notification", true)
}

@ModMachineRecipe(id = "consume_treetap_shaped", recipeClass = ConsumeTreetapShapedRecipe::class)
object ConsumeTreetapShapedRecipeSerializer : ic2_120.content.recipes.RecipeSerializerHolder26<ConsumeTreetapShapedRecipe>() {
    override fun codec(): MapCodec<ConsumeTreetapShapedRecipe> = ic2_120.content.recipes.RecipeCodecBridge.jsonMapCodec(
        "ic2_120:consume_treetap_shaped",
        ::decodeConsumeTreetapShapedJson,
        ::encodeConsumeTreetapShapedJson
    )

    // TODO(1.21.1): 配方不走网络同步（双端各自读 datapack），流编解码仅做占位实现
    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ConsumeTreetapShapedRecipe> =
        StreamCodec.ofMember(
            { recipe: ConsumeTreetapShapedRecipe, buf: RegistryFriendlyByteBuf ->
                CraftingRecipeNetwork26.writeShaped(buf, recipe.patternRows, recipe.patternKeys, recipe.recipeResult)
            },
            { buf: RegistryFriendlyByteBuf ->
                val (rows, keys, result) = CraftingRecipeNetwork26.readShaped(buf)
                ConsumeTreetapShapedRecipe(rows, keys, result)
            }
        )
}

object ConsumeTreetapShapedRecipeDatagen {
    fun offer(
        exporter: net.minecraft.data.recipes.RecipeOutput,
        recipeId: Identifier,
        result: Item,
        pattern: List<String>,
        keys: Map<Char, Item>,
        count: Int = 1,
        category: String = "misc"
    ) {
        val keyMap = linkedMapOf<Char, Ingredient>()
        for ((charKey, item) in keys) keyMap[charKey] = Ingredient.of(item)
        val recipe = ConsumeTreetapShapedRecipe(pattern, keyMap, ItemStack(result, count))
        exporter.accept(recipeId, recipe, null)
    }
}

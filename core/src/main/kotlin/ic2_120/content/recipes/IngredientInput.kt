package ic2_120.content.recipes

import com.google.gson.JsonObject
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey

/**
 * 机器/合成表输入的统一表达：单 Item 或 Tag 任选其一。
 *
 * 序列化到 JSON 时：
 * - Item: `{ "item": "<id>" }`
 * - Tag:  `{ "tag": "<id>" }`
 *
 * 通过工厂 [item] / [tag] 构造，便于在 Entry 中使用。
 * Tag 必须显式提供 [TagInput.displayItem] 用于 JEI 等需要 ItemStack 的展示场景。
 *
 * 使用示例：
 * ```kotlin
 * Entry("iron_ingot_to_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, IronIngot::class.instance()), IronDust::class.instance(), 1)
 * Entry("stone_to_cobblestone", IngredientInput.item(Items.STONE), Items.COBBLESTONE, 1)
 * ```
 */
sealed class IngredientInput {
    abstract fun toJson(): JsonObject
    abstract fun toIngredient(): Ingredient
    abstract fun toItemStack(count: Int): ItemStack
    fun toItemStacks(count: Int): List<ItemStack> =
        toIngredient().matchingStacks.map { stack ->
            stack.copy().also { it.count = count }
        }.ifEmpty {
            listOf(toItemStack(count))
        }

    data class ItemInput(val item: Item) : IngredientInput() {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty("item", Registries.ITEM.getId(item).toString())
        }
        override fun toIngredient(): Ingredient = Ingredient.ofItems(item)
        override fun toItemStack(count: Int): ItemStack = ItemStack(item, count)
    }

    /**
     * @param displayItem 用于 JEI 等展示场景的代表性物品（项目自身物品，会出现在 tag 中）
     */
    data class TagInput(val tag: TagKey<Item>, val displayItem: Item) : IngredientInput() {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty("tag", tag.id.toString())
        }
        override fun toIngredient(): Ingredient = Ingredient.fromTag(tag)
        override fun toItemStack(count: Int): ItemStack = ItemStack(displayItem, count)
    }

    companion object {
        fun item(item: Item): IngredientInput = ItemInput(item)
        fun tag(tag: TagKey<Item>, displayItem: Item): IngredientInput = TagInput(tag, displayItem)
    }
}

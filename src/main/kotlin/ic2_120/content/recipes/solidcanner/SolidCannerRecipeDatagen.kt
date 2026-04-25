package ic2_120.content.recipes.solidcanner

import com.google.gson.JsonObject
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.EmptyTinCanItem
import ic2_120.content.item.FilledTinCanItem
import ic2_120.content.item.Mox
import ic2_120.content.item.MoxFuelRodItem
import ic2_120.content.item.Uranium
import ic2_120.content.item.UraniumFuelRodItem
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer

/**
 * 固体装罐机配方数据生成。
 *
 * 食物装罐：空锡罐 + 食物 -> 罐装食物（满锡罐）
 * 燃料棒装填：空燃料棒 + 核燃料 -> 燃料棒
 */
object SolidCannerRecipeDatagen {

    data class Entry(
        val name: String,
        val slot0Ingredient: Item,
        val slot0Count: Int,
        val slot1Ingredient: Item,
        val slot1Count: Int,
        val outputItem: Item,
        val outputCount: Int
    )

    private val entries = listOf(
        // ===== 食物装罐 =====
        // 牛排 8
        Entry("food_cooked_beef", EmptyTinCanItem::class.instance(), 8, Items.COOKED_BEEF, 1,
              FilledTinCanItem::class.instance(), 8),
        // 毒马铃薯×2 -> 1
        Entry("food_poisonous_potato", EmptyTinCanItem::class.instance(), 1, Items.POISONOUS_POTATO, 2,
              FilledTinCanItem::class.instance(), 1),
        // 曲奇 2
        Entry("food_cookie", EmptyTinCanItem::class.instance(), 2, Items.COOKIE, 1,
              FilledTinCanItem::class.instance(), 2),
        // 熟鱼 5 (鳕鱼、鲑鱼)
        Entry("food_cooked_cod", EmptyTinCanItem::class.instance(), 5, Items.COOKED_COD, 1,
              FilledTinCanItem::class.instance(), 5),
        Entry("food_cooked_salmon", EmptyTinCanItem::class.instance(), 5, Items.COOKED_SALMON, 1,
              FilledTinCanItem::class.instance(), 5),
        // 生鱼 2
        Entry("food_cod", EmptyTinCanItem::class.instance(), 2, Items.COD, 1,
              FilledTinCanItem::class.instance(), 2),
        Entry("food_salmon", EmptyTinCanItem::class.instance(), 2, Items.SALMON, 1,
              FilledTinCanItem::class.instance(), 2),
        Entry("food_tropical_fish", EmptyTinCanItem::class.instance(), 2, Items.TROPICAL_FISH, 1,
              FilledTinCanItem::class.instance(), 2),
        Entry("food_pufferfish", EmptyTinCanItem::class.instance(), 2, Items.PUFFERFISH, 1,
              FilledTinCanItem::class.instance(), 2),
        // 蛋糕 12
        Entry("food_cake", EmptyTinCanItem::class.instance(), 12, Items.CAKE, 1,
              FilledTinCanItem::class.instance(), 12),
        // 西瓜片 2
        Entry("food_melon_slice", EmptyTinCanItem::class.instance(), 2, Items.MELON_SLICE, 1,
              FilledTinCanItem::class.instance(), 2),
        // 生鸡肉 2
        Entry("food_chicken", EmptyTinCanItem::class.instance(), 2, Items.CHICKEN, 1,
              FilledTinCanItem::class.instance(), 2),
        // 胡萝卜 4
        Entry("food_carrot", EmptyTinCanItem::class.instance(), 4, Items.CARROT, 1,
              FilledTinCanItem::class.instance(), 4),
        // 生猪排 3
        Entry("food_porkchop", EmptyTinCanItem::class.instance(), 3, Items.PORKCHOP, 1,
              FilledTinCanItem::class.instance(), 3),
        // 腐肉×2 -> 1
        Entry("food_rotten_flesh", EmptyTinCanItem::class.instance(), 1, Items.ROTTEN_FLESH, 2,
              FilledTinCanItem::class.instance(), 1),
        // 生牛肉 3
        Entry("food_beef", EmptyTinCanItem::class.instance(), 3, Items.BEEF, 1,
              FilledTinCanItem::class.instance(), 3),
        // 南瓜派 6
        Entry("food_pumpkin_pie", EmptyTinCanItem::class.instance(), 6, Items.PUMPKIN_PIE, 1,
              FilledTinCanItem::class.instance(), 6),
        // 马铃薯 1
        Entry("food_potato", EmptyTinCanItem::class.instance(), 1, Items.POTATO, 1,
              FilledTinCanItem::class.instance(), 1),
        // 蘑菇煲 6
        Entry("food_mushroom_stew", EmptyTinCanItem::class.instance(), 6, Items.MUSHROOM_STEW, 1,
              FilledTinCanItem::class.instance(), 6),
        // 苹果 4
        Entry("food_apple", EmptyTinCanItem::class.instance(), 4, Items.APPLE, 1,
              FilledTinCanItem::class.instance(), 4),
        Entry("food_golden_apple", EmptyTinCanItem::class.instance(), 4, Items.GOLDEN_APPLE, 1,
              FilledTinCanItem::class.instance(), 4),
        Entry("food_enchanted_golden_apple", EmptyTinCanItem::class.instance(), 4, Items.ENCHANTED_GOLDEN_APPLE, 1,
              FilledTinCanItem::class.instance(), 4),
        // 熟猪排 8
        Entry("food_cooked_porkchop", EmptyTinCanItem::class.instance(), 8, Items.COOKED_PORKCHOP, 1,
              FilledTinCanItem::class.instance(), 8),
        // 熟鸡肉 6
        Entry("food_cooked_chicken", EmptyTinCanItem::class.instance(), 6, Items.COOKED_CHICKEN, 1,
              FilledTinCanItem::class.instance(), 6),
        // 面包 5
        Entry("food_bread", EmptyTinCanItem::class.instance(), 5, Items.BREAD, 1,
              FilledTinCanItem::class.instance(), 5),
        // 烤马铃薯 6
        Entry("food_baked_potato", EmptyTinCanItem::class.instance(), 6, Items.BAKED_POTATO, 1,
              FilledTinCanItem::class.instance(), 6),
        // 甜菜根 1
        Entry("food_beetroot", EmptyTinCanItem::class.instance(), 1, Items.BEETROOT, 1,
              FilledTinCanItem::class.instance(), 1),
        // 甜菜根汤 6
        Entry("food_beetroot_soup", EmptyTinCanItem::class.instance(), 6, Items.BEETROOT_SOUP, 1,
              FilledTinCanItem::class.instance(), 6),
        // 兔肉 2
        Entry("food_rabbit", EmptyTinCanItem::class.instance(), 2, Items.RABBIT, 1,
              FilledTinCanItem::class.instance(), 2),
        // 熟兔肉 5
        Entry("food_cooked_rabbit", EmptyTinCanItem::class.instance(), 5, Items.COOKED_RABBIT, 1,
              FilledTinCanItem::class.instance(), 5),
        // 兔肉煲 6
        Entry("food_rabbit_stew", EmptyTinCanItem::class.instance(), 6, Items.RABBIT_STEW, 1,
              FilledTinCanItem::class.instance(), 6),
        // 甜浆果 1
        Entry("food_sweet_berries", EmptyTinCanItem::class.instance(), 1, Items.SWEET_BERRIES, 1,
              FilledTinCanItem::class.instance(), 1),
        // 发光浆果 1
        Entry("food_glow_berries", EmptyTinCanItem::class.instance(), 1, Items.GLOW_BERRIES, 1,
              FilledTinCanItem::class.instance(), 1),
        // 蜘蛛眼 1（毒物）
        Entry("food_spider_eye", EmptyTinCanItem::class.instance(), 1, Items.SPIDER_EYE, 1,
              FilledTinCanItem::class.instance(), 1),

        // ===== 燃料棒装填 =====
        // 空燃料棒 + 浓缩铀 -> 铀燃料棒
        Entry("fuel_rod_uranium",
              EmptyFuelRodItem::class.instance(), 1,
              Uranium::class.instance(), 1,
              UraniumFuelRodItem::class.instance(), 1),
        // 空燃料棒 + MOX -> MOX燃料棒
        Entry("fuel_rod_mox",
              EmptyFuelRodItem::class.instance(), 1,
              Mox::class.instance(), 1,
              MoxFuelRodItem::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeExporter>) {
        entries.forEach { entry ->
            SolidCannerRecipeExporter(
                recipeId = Identifier.of("ic2_120", "solid_canning/${entry.name}"),
                slot0Item = entry.slot0Ingredient,
                slot0Count = entry.slot0Count,
                slot1Item = entry.slot1Ingredient,
                slot1Count = entry.slot1Count,
                outputItem = entry.outputItem,
                outputCount = entry.outputCount
            ).also(exporter::accept)
        }
    }

    private class SolidCannerRecipeExporter(
        private val recipeId: Identifier,
        private val slot0Item: Item,
        private val slot0Count: Int,
        private val slot1Item: Item,
        private val slot1Count: Int,
        private val outputItem: Item,
        private val outputCount: Int
    ) : RecipeExporter {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.recipeType(SolidCannerRecipe::class)}")

            // 槽0 成分（锡罐 或 空燃料棒）
            val slot0Ingredient = JsonObject()
            slot0Ingredient.addProperty("item", Registries.ITEM.getId(slot0Item).toString())
            json.add("slot0_ingredient", slot0Ingredient)
            json.addProperty("slot0_count", slot0Count)

            // 槽1 成分（食物 或 核燃料）
            val slot1Ingredient = JsonObject()
            slot1Ingredient.addProperty("item", Registries.ITEM.getId(slot1Item).toString())
            json.add("slot1_ingredient", slot1Ingredient)
            json.addProperty("slot1_count", slot1Count)

            // 产出
            val result = JsonObject()
            result.addProperty("item", Registries.ITEM.getId(outputItem).toString())
            result.addProperty("count", outputCount)
            json.add("result", result)
        }

        override fun getSerializer() = ModMachineRecipes.recipeSerializer(SolidCannerRecipe::class)

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}

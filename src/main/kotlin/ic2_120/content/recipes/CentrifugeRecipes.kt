package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 热能离心机配方。
 *
 * 输入 + 最低热量要求 -> 多个输出
 */
data class CentrifugeRecipe(
    val inputItem: Item,
    val inputCount: Int,
    val minHeat: Int,
    val outputs: List<ItemStack>
)

object CentrifugeRecipes {

    private val recipes = mutableListOf<CentrifugeRecipe>()

    init {
        fun stack(id: String, count: Int = 1): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }

        // 圆石 100 -> 石粉
        add("minecraft:cobblestone", 1, 100, listOf(stack("ic2_120:stone_dust")))

        // 下界石英X2 250 -> 小撮锂粉
        add("minecraft:quartz", 2, 250, listOf(stack("ic2_120:small_lithium_dust")))

        // 粉碎铜矿石 500 -> 小撮锡粉,铜粉,石粉
        add("ic2_120:crushed_copper", 1, 500, listOf(
            stack("ic2_120:small_tin_dust"),
            stack("ic2_120:copper_dust"),
            stack("ic2_120:stone_dust")
        ))

        // 粉碎锡矿石 1000 -> 小撮铁粉,锡粉,石粉
        add("ic2_120:crushed_tin", 1, 1000, listOf(
            stack("ic2_120:small_iron_dust"),
            stack("ic2_120:tin_dust"),
            stack("ic2_120:stone_dust")
        ))

        // 粉碎铁矿石 1500 -> 小撮金粉,铁粉,石粉
        add("ic2_120:crushed_iron", 1, 1500, listOf(
            stack("ic2_120:small_gold_dust"),
            stack("ic2_120:iron_dust"),
            stack("ic2_120:stone_dust")
        ))

        // 粉碎金矿石 2000 -> 小撮银粉,金粉,石粉
        add("ic2_120:crushed_gold", 1, 2000, listOf(
            stack("ic2_120:small_silver_dust"),
            stack("ic2_120:gold_dust"),
            stack("ic2_120:stone_dust")
        ))

        // 粉碎铀矿石 3000 -> 小撮铀-235, 铀-238X4, 石粉
        add("ic2_120:crushed_uranium", 1, 3000, listOf(
            stack("ic2_120:small_uranium_235"),
            stack("ic2_120:small_uranium_238", 4),
            stack("ic2_120:stone_dust")
        ))

        // 粉碎铅矿石 2000 -> 铅粉, 石粉
        add("ic2_120:crushed_lead", 1, 2000, listOf(
            stack("ic2_120:lead_dust"),
            stack("ic2_120:stone_dust")
        ))

        // 纯净的粉碎铜矿石 500 -> 小撮锡粉,铜粉
        add("ic2_120:purified_copper", 1, 500, listOf(
            stack("ic2_120:small_tin_dust"),
            stack("ic2_120:copper_dust")
        ))

        // 纯净的粉碎锡矿石 1000 -> 小撮铁粉,锡粉
        add("ic2_120:purified_tin", 1, 1000, listOf(
            stack("ic2_120:small_iron_dust"),
            stack("ic2_120:tin_dust")
        ))

        // 纯净的粉碎铁矿石 1500 -> 小撮金粉,铁粉
        add("ic2_120:purified_iron", 1, 1500, listOf(
            stack("ic2_120:small_gold_dust"),
            stack("ic2_120:iron_dust")
        ))

        // 纯净的粉碎金矿石 2000 -> 小撮银粉,金粉
        add("ic2_120:purified_gold", 1, 2000, listOf(
            stack("ic2_120:small_silver_dust"),
            stack("ic2_120:gold_dust")
        ))

        // 纯净的粉碎铀矿石 3000 -> 小撮铀-235X1, 铀-238X6 (1.12实测)
        add("ic2_120:purified_uranium", 1, 3000, listOf(
            stack("ic2_120:small_uranium_235"),
            stack("ic2_120:small_uranium_238", 6)
        ))

        // 纯净的粉碎铅矿石 2000 -> 小撮铜粉,铅粉
        add("ic2_120:purified_lead", 1, 2000, listOf(
            stack("ic2_120:small_copper_dust"),
            stack("ic2_120:lead_dust")
        ))

        // 燃料棒(枯竭铀) 4000 -> 小撮钚, 铀-238X4,铁粉X1
        add("ic2_120:depleted_uranium_fuel_rod", 1, 4000, listOf(
            stack("ic2_120:small_plutonium"),
            stack("ic2_120:small_uranium_238", 4),
            stack("ic2_120:iron_dust")
        ))

        // 双联燃料棒(枯竭铀) 4000 -> 小撮钚X2, 铀-238X8, 铁粉X3
        add("ic2_120:depleted_dual_uranium_fuel_rod", 1, 4000, listOf(
            stack("ic2_120:small_plutonium", 2),
            stack("ic2_120:small_uranium_238", 8),
            stack("ic2_120:iron_dust", 3)
        ))

        // 四联燃料棒(枯竭铀) 4000 -> 小撮钚X4, 铀-238X16, 铁粉X7
        add("ic2_120:depleted_quad_uranium_fuel_rod", 1, 4000, listOf(
            stack("ic2_120:small_plutonium", 4),
            stack("ic2_120:small_uranium_238", 16),
            stack("ic2_120:iron_dust", 7)
        ))

        // 燃料棒(枯竭MOX) 5000 -> 小撮钚, 钚X3, 铁粉X1
        add("ic2_120:depleted_mox_fuel_rod", 1, 5000, listOf(
            stack("ic2_120:small_plutonium"),
            stack("ic2_120:plutonium", 3),
            stack("ic2_120:iron_dust")
        ))

        // 双联燃料棒(枯竭MOX) 5000 -> 小撮钚X2, 钚X6, 铁粉X3
        add("ic2_120:depleted_dual_mox_fuel_rod", 1, 5000, listOf(
            stack("ic2_120:small_plutonium", 2),
            stack("ic2_120:plutonium", 6),
            stack("ic2_120:iron_dust", 3)
        ))

        // 四联燃料棒(枯竭MOX) 5000 -> 小撮钚X4, 钚X12, 铁粉X7
        add("ic2_120:depleted_quad_mox_fuel_rod", 1, 5000, listOf(
            stack("ic2_120:small_plutonium", 4),
            stack("ic2_120:plutonium", 12),
            stack("ic2_120:iron_dust", 7)
        ))

        // 放射性同位素燃料靶丸 5000 -> 钚X3,铁粉X54
        add("ic2_120:rtg_pellet", 1, 5000, listOf(
            stack("ic2_120:plutonium", 3),
            stack("ic2_120:iron_dust", 54)
        ))

        // 炉渣 1500 -> 小撮金粉,碳粉X5 (碳粉用煤粉代替)
        add("ic2_120:slag", 1, 1500, listOf(
            stack("ic2_120:small_gold_dust"),
            stack("ic2_120:coal_dust", 5)
        ))

        // 粘土粉X4 250 -> 二氧化硅粉
        add("ic2_120:clay_dust", 4, 250, listOf(stack("ic2_120:silicon_dioxide_dust")))
    }

    private fun add(inputId: String, inputCount: Int, minHeat: Int, outputs: List<ItemStack>) {
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (outputs.isEmpty()) return
        recipes.add(CentrifugeRecipe(item, inputCount, minHeat, outputs))
    }

    fun getRecipe(input: ItemStack): CentrifugeRecipe? {
        if (input.isEmpty) return null
        return recipes.find { it.inputItem == input.item && input.count >= it.inputCount }
    }

    fun isValidInput(input: ItemStack): Boolean = getRecipe(input) != null
}

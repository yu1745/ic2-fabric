package ic2_120.content.recipes.centrifuge

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ic2_120.content.item.*
import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.registry.instance
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 热能离心机配方数据生成
 */
object CentrifugeRecipeDatagen {
    data class OutputEntry(
        val item: Item,
        val count: Int
    )

    data class Entry(
        val name: String,
        val input: IngredientInput,
        val inputCount: Int,
        val minHeat: Int,
        val outputs: List<OutputEntry>
    )

    private val entries = listOf(
        // 圆石 100 -> 石粉
        Entry(
            "cobblestone_to_stone_dust",
            IngredientInput.item(Items.COBBLESTONE), 1, 100,
            listOf(OutputEntry(StoneDust::class.instance(), 1))
        ),

        // 下界石英X2 250 -> 小撮锂粉
        Entry(
            "quartz_to_small_lithium_dust",
            IngredientInput.item(Items.QUARTZ), 2, 250,
            listOf(OutputEntry(SmallLithiumDust::class.instance(), 1))
        ),

        // 粉碎铜矿石 500 -> 小撮锡粉,铜粉,石粉
        Entry(
            "crushed_copper",
            IngredientInput.item(CrushedCopper::class.instance()), 1, 500,
            listOf(
                OutputEntry(SmallTinDust::class.instance(), 1),
                OutputEntry(CopperDust::class.instance(), 1),
                OutputEntry(StoneDust::class.instance(), 1)
            )
        ),

        // 粉碎锡矿石 1000 -> 小撮铁粉,锡粉,石粉
        Entry(
            "crushed_tin",
            IngredientInput.item(CrushedTin::class.instance()), 1, 1000,
            listOf(
                OutputEntry(SmallIronDust::class.instance(), 1),
                OutputEntry(TinDust::class.instance(), 1),
                OutputEntry(StoneDust::class.instance(), 1)
            )
        ),

        // 粉碎铁矿石 1500 -> 小撮金粉,铁粉,石粉
        Entry(
            "crushed_iron",
            IngredientInput.item(CrushedIron::class.instance()), 1, 1500,
            listOf(
                OutputEntry(SmallGoldDust::class.instance(), 1),
                OutputEntry(IronDust::class.instance(), 1),
                OutputEntry(StoneDust::class.instance(), 1)
            )
        ),

        // 粉碎金矿石 2000 -> 小撮银粉,金粉,石粉
        Entry(
            "crushed_gold",
            IngredientInput.item(CrushedGold::class.instance()), 1, 2000,
            listOf(
                OutputEntry(SmallSilverDust::class.instance(), 1),
                OutputEntry(GoldDust::class.instance(), 1),
                OutputEntry(StoneDust::class.instance(), 1)
            )
        ),

        // 粉碎铀矿石 3000 -> 小撮铀-235, 铀-238×4, 石粉
        Entry(
            "crushed_uranium",
            IngredientInput.item(CrushedUranium::class.instance()), 1, 3000,
            listOf(
                OutputEntry(SmallUranium235::class.instance(), 1),
                OutputEntry(Uranium238::class.instance(), 4),
                OutputEntry(StoneDust::class.instance(), 1)
            )
        ),

        // 粉碎铅矿石 2000 -> 铅粉, 石粉
        Entry(
            "crushed_lead",
            IngredientInput.item(CrushedLead::class.instance()), 1, 2000,
            listOf(
                OutputEntry(LeadDust::class.instance(), 1),
                OutputEntry(StoneDust::class.instance(), 1)
            )
        ),

        // 纯净的粉碎铜矿石 500 -> 小撮锡粉,铜粉
        Entry(
            "purified_copper",
            IngredientInput.item(PurifiedCopper::class.instance()), 1, 500,
            listOf(
                OutputEntry(SmallTinDust::class.instance(), 1),
                OutputEntry(CopperDust::class.instance(), 1)
            )
        ),

        // 纯净的粉碎锡矿石 1000 -> 小撮铁粉,锡粉
        Entry(
            "purified_tin",
            IngredientInput.item(PurifiedTin::class.instance()), 1, 1000,
            listOf(
                OutputEntry(SmallIronDust::class.instance(), 1),
                OutputEntry(TinDust::class.instance(), 1)
            )
        ),

        // 纯净的粉碎铁矿石 1500 -> 小撮金粉,铁粉
        Entry(
            "purified_iron",
            IngredientInput.item(PurifiedIron::class.instance()), 1, 1500,
            listOf(
                OutputEntry(SmallGoldDust::class.instance(), 1),
                OutputEntry(IronDust::class.instance(), 1)
            )
        ),

        // 纯净的粉碎金矿石 2000 -> 小撮银粉,金粉
        Entry(
            "purified_gold",
            IngredientInput.item(PurifiedGold::class.instance()), 1, 2000,
            listOf(
                OutputEntry(SmallSilverDust::class.instance(), 1),
                OutputEntry(GoldDust::class.instance(), 1)
            )
        ),

        // 纯净的粉碎铀矿石 3000 -> 小撮铀-235×1, 铀-238×6
        Entry(
            "purified_uranium",
            IngredientInput.item(PurifiedUranium::class.instance()), 1, 3000,
            listOf(
                OutputEntry(SmallUranium235::class.instance(), 1),
                OutputEntry(Uranium238::class.instance(), 6)
            )
        ),

        // 纯净的粉碎铅矿石 2000 -> 小撮铜粉,铅粉
        Entry(
            "purified_lead",
            IngredientInput.item(PurifiedLead::class.instance()), 1, 2000,
            listOf(
                OutputEntry(SmallCopperDust::class.instance(), 1),
                OutputEntry(LeadDust::class.instance(), 1)
            )
        ),

        // 燃料棒(枯竭铀) 4000 -> 小撮钚, 铀-238X4,铁粉X1
        Entry(
            "depleted_uranium_fuel_rod",
            IngredientInput.item(DepletedUraniumFuelRodItem::class.instance()), 1, 4000,
            listOf(
                OutputEntry(SmallPlutonium::class.instance(), 1),
                OutputEntry(Uranium238::class.instance(), 4),
                OutputEntry(IronDust::class.instance(), 1)
            )
        ),

        // 双联燃料棒(枯竭铀) 4000 -> 小撮钚X2, 铀-238X8, 铁粉X3
        Entry(
            "depleted_dual_uranium_fuel_rod",
            IngredientInput.item(DepletedDualUraniumFuelRodItem::class.instance()), 1, 4000,
            listOf(
                OutputEntry(SmallPlutonium::class.instance(), 2),
                OutputEntry(Uranium238::class.instance(), 8),
                OutputEntry(IronDust::class.instance(), 3)
            )
        ),

        // 四联燃料棒(枯竭铀) 4000 -> 小撮钚X4, 铀-238X16, 铁粉X7
        Entry(
            "depleted_quad_uranium_fuel_rod",
            IngredientInput.item(DepletedQuadUraniumFuelRodItem::class.instance()), 1, 4000,
            listOf(
                OutputEntry(SmallPlutonium::class.instance(), 4),
                OutputEntry(Uranium238::class.instance(), 16),
                OutputEntry(IronDust::class.instance(), 7)
            )
        ),

        // 燃料棒(枯竭MOX) 5000 -> 小撮钚, 钚X3, 铁粉X1
        Entry(
            "depleted_mox_fuel_rod",
            IngredientInput.item(DepletedMoxFuelRodItem::class.instance()), 1, 5000,
            listOf(
                OutputEntry(SmallPlutonium::class.instance(), 1),
                OutputEntry(Plutonium::class.instance(), 3),
                OutputEntry(IronDust::class.instance(), 1)
            )
        ),

        // 双联燃料棒(枯竭MOX) 5000 -> 小撮钚X2, 钚X6, 铁粉X3
        Entry(
            "depleted_dual_mox_fuel_rod",
            IngredientInput.item(DepletedDualMoxFuelRodItem::class.instance()), 1, 5000,
            listOf(
                OutputEntry(SmallPlutonium::class.instance(), 2),
                OutputEntry(Plutonium::class.instance(), 6),
                OutputEntry(IronDust::class.instance(), 3)
            )
        ),

        // 四联燃料棒(枯竭MOX) 5000 -> 小撮钚X4, 钚X12, 铁粉X7
        Entry(
            "depleted_quad_mox_fuel_rod",
            IngredientInput.item(DepletedQuadMoxFuelRodItem::class.instance()), 1, 5000,
            listOf(
                OutputEntry(SmallPlutonium::class.instance(), 4),
                OutputEntry(Plutonium::class.instance(), 12),
                OutputEntry(IronDust::class.instance(), 7)
            )
        ),

        // 放射性同位素燃料靶丸 5000 -> 钚X3,铁粉X54
        Entry(
            "rtg_pellet",
            IngredientInput.item(RtgPellet::class.instance()), 1, 5000,
            listOf(
                OutputEntry(Plutonium::class.instance(), 3),
                OutputEntry(IronDust::class.instance(), 54)
            )
        ),

        // 炉渣 1500 -> 小撮金粉,碳粉X5 (碳粉用煤粉代替)
        Entry(
            "slag",
            IngredientInput.item(Slag::class.instance()), 1, 1500,
            listOf(
                OutputEntry(SmallGoldDust::class.instance(), 1),
                OutputEntry(CoalDust::class.instance(), 5)
            )
        ),

        // 粘土粉X4 250 -> 二氧化硅粉
        Entry(
            "clay_dust_to_silicon_dioxide",
            IngredientInput.item(ClayDust::class.instance()), 4, 250,
            listOf(OutputEntry(SiliconDioxideDust::class.instance(), 1))
        ),

        // 泥炭矿 200 -> 煤炭 + 糠
        Entry(
            "peat_ore",
            IngredientInput.item(Registries.ITEM.get(Identifier.of("ic2_120", "peat_ore"))), 1, 200,
            listOf(
                OutputEntry(Items.COAL, 1),
                OutputEntry(BioChaff::class.instance(), 1)
            )
        )
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "centrifuging/${entry.name}")
            exporter.accept(
                id,
                CentrifugeRecipe(
                    id,
                    entry.input.toIngredient(),
                    entry.inputCount,
                    entry.minHeat,
                    entry.outputs.map { ItemStack(it.item, it.count) }
                ),
                null
            )
        }
    }
}

package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 洗矿机配方输出结果。
 * @param output1 主要输出（纯净的粉碎矿石）
 * @param output2 固定副产物（石粉）
 * @param output3 小撮金属粉（2个）
 */
data class OreWashingResult(
    val output1: ItemStack,  // 纯净的粉碎矿石
    val output2: ItemStack,  // 石粉
    val output3: ItemStack   // 小撮金属粉（2个）
)

/**
 * 洗矿机配方：1 粉碎矿石 + 1 桶水 → 1 纯净的粉碎矿石 + 1 石粉 + 2 小撮金属粉
 */
object OreWashingPlantRecipes {

    private val cache = mutableMapOf<Item, OreWashingResult>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }

        // 粉碎矿石 -> 纯净的粉碎矿石 + 石粉 + 2 小撮金属粉
        add("ic2_120:crushed_iron", OreWashingResult(
            stack("ic2_120:purified_iron", 1),
            stack("ic2_120:stone_dust", 1),
            stack("ic2_120:small_iron_dust", 2)
        ))
        add("ic2_120:crushed_gold", OreWashingResult(
            stack("ic2_120:purified_gold", 1),
            stack("ic2_120:stone_dust", 1),
            stack("ic2_120:small_gold_dust", 2)
        ))
        add("ic2_120:crushed_copper", OreWashingResult(
            stack("ic2_120:purified_copper", 1),
            stack("ic2_120:stone_dust", 1),
            stack("ic2_120:small_copper_dust", 2)
        ))
        add("ic2_120:crushed_tin", OreWashingResult(
            stack("ic2_120:purified_tin", 1),
            stack("ic2_120:stone_dust", 1),
            stack("ic2_120:small_tin_dust", 2)
        ))
        add("ic2_120:crushed_lead", OreWashingResult(
            stack("ic2_120:purified_lead", 1),
            stack("ic2_120:stone_dust", 1),
            stack("ic2_120:small_lead_dust", 2)
        ))
        add("ic2_120:crushed_silver", OreWashingResult(
            stack("ic2_120:purified_silver", 1),
            stack("ic2_120:stone_dust", 1),
            stack("ic2_120:small_silver_dust", 2)
        ))
        add("ic2_120:crushed_uranium", OreWashingResult(
            stack("ic2_120:purified_uranium", 1),
            stack("ic2_120:stone_dust", 1),
            stack("ic2_120:small_uranium_dust", 2)
        ))
    }

    private fun add(inputId: String, result: OreWashingResult) {
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (result.output1.isEmpty || result.output2.isEmpty || result.output3.isEmpty) return
        cache[item] = result
    }

    fun getOutput(input: ItemStack): OreWashingResult? {
        if (input.isEmpty) return null
        return cache[input.item]
    }
}

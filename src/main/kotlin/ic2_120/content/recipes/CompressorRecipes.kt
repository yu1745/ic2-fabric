package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 压缩机配方：输入物品 + 数量 -> 输出物品及数量。
 * 配方为“每 N 个输入 -> 1 个输出”，输入槽每次消耗 inputCount 个。
 *
 * 压缩机规格（无超频时）：
 * - 工作功率：2 EU/t
 * - 压缩每个物品耗时：15 秒
 * - 每个物品总耗电量：600 EU
 * - 最大输入电压：32 EU/t
 * - 过压爆炸：未安装高压升级时，通入过大电压会导致机器爆炸
 */
object CompressorRecipes {

    private data class Recipe(val inputCount: Int, val output: ItemStack)

    private val cache = mutableMapOf<Item, Recipe>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }
        // 4 煤炭 -> 1 煤炭块（煤球 -> 压缩煤球的前置）
        add("minecraft:coal", 4, stack("minecraft:coal_block", 1))
        // 9 铁锭 -> 1 铁块（金属锭 -> 金属块）
        add("minecraft:iron_ingot", 9, stack("minecraft:iron_block", 1))
        // 9 金锭 -> 1 金块（金属锭 -> 金属块）
        add("minecraft:gold_ingot", 9, stack("minecraft:gold_block", 1))
        // 9 铜锭 -> 1 铜块（金属锭 -> 金属块）
        add("minecraft:copper_ingot", 9, stack("minecraft:copper_block", 1))
        // 9 IC2 铜锭 -> 1 铜块（金属锭 -> 金属块）
        add("ic2_120:copper_ingot", 9, stack("ic2_120:copper_block", 1))
        // 9 锡锭 -> 1 锡块（金属锭 -> 金属块）
        add("ic2_120:tin_ingot", 9, stack("ic2_120:tin_block", 1))
        // 9 青铜锭 -> 1 青铜块（金属锭 -> 金属块）
        add("ic2_120:bronze_ingot", 9, stack("ic2_120:bronze_block", 1))
        // 4 沙子 -> 1 沙石（沙子 -> 沙石）
        add("minecraft:sand", 4, stack("minecraft:sandstone", 1))
        // 4 红沙 -> 1 红沙石（沙子 -> 沙石）
        add("minecraft:red_sand", 4, stack("minecraft:red_sandstone", 1))
        // 4 粘土球 -> 1 砖（粘土 -> 粘土块）
        add("minecraft:clay_ball", 4, stack("minecraft:brick", 1))
        // 4 雪球 -> 1 雪块（雪球 -> 雪块）
        add("minecraft:snowball", 4, stack("minecraft:snow_block", 1))
        // 4 萤石粉 -> 1 萤石（萤石粉 -> 萤石）
        add("minecraft:glowstone_dust", 4, stack("minecraft:glowstone", 1))
        // 4 青金石 -> 1 青金石块
        add("minecraft:lapis_lazuli", 4, stack("minecraft:lapis_block", 1))
        // 1 黑曜石 -> 1 黑曜石板
        add("minecraft:obsidian", 1, stack("ic2_120:obsidian_plate", 1))
        // 9 青铜板 -> 1 致密青铜板（矿物板 -> 致密板）
        add("ic2_120:bronze_plate", 9, stack("ic2_120:dense_bronze_plate", 1))
        // 9 铜板 -> 1 致密铜板（矿物板 -> 致密板）
        add("ic2_120:copper_plate", 9, stack("ic2_120:dense_copper_plate", 1))
        // 9 金板 -> 1 致密金板（矿物板 -> 致密板）
        add("ic2_120:gold_plate", 9, stack("ic2_120:dense_gold_plate", 1))
        // 9 铁板 -> 1 致密铁板（矿物板 -> 致密板）
        add("ic2_120:iron_plate", 9, stack("ic2_120:dense_iron_plate", 1))
        // 9 青金石板 -> 1 致密青金石板（矿物板 -> 致密板）
        add("ic2_120:lapis_plate", 9, stack("ic2_120:dense_lapis_plate", 1))
        // 9 铅板 -> 1 致密铅板（矿物板 -> 致密板）
        add("ic2_120:lead_plate", 9, stack("ic2_120:dense_lead_plate", 1))
        // 9 黑曜石板 -> 1 致密黑曜石板（矿物板 -> 致密板）
        add("ic2_120:obsidian_plate", 9, stack("ic2_120:dense_obsidian_plate", 1))
        // 9 钢板 -> 1 致密钢板（矿物板 -> 致密板）
        add("ic2_120:steel_plate", 9, stack("ic2_120:dense_steel_plate", 1))
        // 9 锡板 -> 1 致密锡板（矿物板 -> 致密板）
        add("ic2_120:tin_plate", 9, stack("ic2_120:dense_tin_plate", 1))
    }

    private fun add(inputId: String, inputCount: Int, output: ItemStack) {
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (output.isEmpty) return
        cache[item] = Recipe(inputCount, output)
    }

    /** 获取配方：若输入匹配则返回 (需要消耗数量, 输出堆叠)，否则 null。 */
    fun getRecipe(input: ItemStack): Pair<Int, ItemStack>? {
        if (input.isEmpty) return null
        val recipe = cache[input.item] ?: return null
        if (input.count < recipe.inputCount) return null
        return recipe.inputCount to recipe.output.copy()
    }
}

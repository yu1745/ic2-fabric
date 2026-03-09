package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 提取机配方：输入物品 -> 输出物品及数量。
 * 功率 2 EU/t，耗时 20 秒，单次 800 EU。
 *
 * 橡胶提取（与工业先锋塑料片兼容，与 MFR2 生橡胶不兼容）：
 * - 1 粘性树脂 -> 3 橡胶
 * - 1 橡胶原木 -> 1 橡胶（与 MFR2、RP2 橡胶原木兼容）
 * - 1 橡胶木 -> 1 橡胶
 * - 1 橡胶树苗 -> 1 橡胶
 */
object ExtractorRecipes {

    private val cache = mutableMapOf<Item, ItemStack>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            val item = Registries.ITEM.get(ident)
            return if (item != Items.AIR) ItemStack(item, count) else ItemStack.EMPTY
        }
        // === 橡胶类 ===
        add("ic2_120:resin", stack("ic2_120:rubber", 3))
        add("ic2_120:rubber_log", stack("ic2_120:rubber", 1))
        add("ic2_120:stripped_rubber_log", stack("ic2_120:rubber", 1))
        add("ic2_120:rubber_wood", stack("ic2_120:rubber", 1))
        add("ic2_120:stripped_rubber_wood", stack("ic2_120:rubber", 1))
        add("ic2_120:rubber_sapling", stack("ic2_120:rubber", 1))
        addOptional("techreborn:rubber_log", stack("ic2_120:rubber", 1))
        addOptional("techreborn:rubber_log_stripped", stack("ic2_120:rubber", 1))

        // === 染色羊毛 -> 羊毛 ===
        for (color in listOf("orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            add("minecraft:${color}_wool", ItemStack(Items.WHITE_WOOL, 1))
        }

        // === 单元类 ===
        add("ic2_120:air_cell", stack("ic2_120:empty_cell", 1))
        add("ic2_120:bio_cell", stack("ic2_120:biofuel_cell", 1))
        add("ic2_120:compressed_hydrated_coal", stack("ic2_120:coal_fuel_dust", 1))

        // === 块 -> 4 份材料 ===
        add("minecraft:bricks", ItemStack(Items.BRICK, 4))
        add("minecraft:nether_bricks", ItemStack(Items.NETHER_BRICK, 4))
        add("minecraft:clay", ItemStack(Items.CLAY_BALL, 4))
        add("minecraft:snow_block", ItemStack(Items.SNOWBALL, 4))

        // === 其他 ===
        add("minecraft:gunpowder", stack("ic2_120:sulfur_dust", 1))
        add("ic2_120:filled_tin_can", stack("ic2_120:tin_can", 1))
        add("ic2_120:hydrated_tin_dust", stack("ic2_120:iodine", 1))
        add("ic2_120:netherrack_dust", stack("ic2_120:small_sulfur_dust", 1))
    }

    private fun add(inputId: String, output: ItemStack) {
        if (output.isEmpty) return
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        cache[item] = output
    }

    private fun addOptional(inputId: String, output: ItemStack) {
        if (output.isEmpty) return
        val ident = Identifier.tryParse(inputId) ?: return
        if (!Registries.ITEM.containsId(ident)) return
        val item = Registries.ITEM.get(ident)
        if (item != net.minecraft.item.Items.AIR) cache[item] = output
    }

    fun getOutput(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        return cache[input.item]?.copy()
    }
}

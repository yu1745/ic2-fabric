package ic2_120.content.recipes

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 方块切割机配方。
 * - 方块 → 9 个对应板（板/台阶）
 * - 原木 → 6 木板（增产 50%）
 * - 木板 → 6 木棍（2 木板，增产 50%）
 *
 * 材料硬度用于锯片检查：只能切割硬度低于锯片硬度的材料。
 */
object BlockCutterRecipes {

    private data class Recipe(val output: ItemStack, val materialHardness: Float)

    private val cache = mutableMapOf<Item, Recipe>()
    /** 木板→木棍配方（2 木板输入，与 slab 配方冲突，需单独处理） */
    private val stickRecipes = mutableMapOf<Item, Recipe>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }

        // 金属块 → 9 板
        add("minecraft:iron_block", stack("ic2_120:iron_plate", 9), 5.0f)
        add("minecraft:gold_block", stack("ic2_120:gold_plate", 9), 5.0f)
        add("minecraft:copper_block", stack("ic2_120:copper_plate", 9), 3.0f)
        add("ic2_120:tin_block", stack("ic2_120:tin_plate", 9), 3.0f)
        add("ic2_120:bronze_block", stack("ic2_120:bronze_plate", 9), 5.0f)
        add("ic2_120:lead_block", stack("ic2_120:lead_plate", 9), 5.0f)
        add("ic2_120:steel_block", stack("ic2_120:steel_plate", 9), 6.0f)
        add("minecraft:lapis_block", stack("ic2_120:lapis_plate", 9), 3.0f)
        add("minecraft:obsidian", stack("ic2_120:obsidian_plate", 9), 50.0f)

        // 石头类 → 9 台阶
        add("minecraft:stone", stack("minecraft:stone_slab", 9), 1.5f)
        add("minecraft:cobblestone", stack("minecraft:cobblestone_slab", 9), 2.0f)
        add("minecraft:sandstone", stack("minecraft:sandstone_slab", 9), 0.8f)
        add("minecraft:cut_sandstone", stack("minecraft:cut_sandstone_slab", 9), 0.8f)
        add("minecraft:smooth_sandstone", stack("minecraft:smooth_sandstone_slab", 9), 0.8f)
        add("minecraft:red_sandstone", stack("minecraft:red_sandstone_slab", 9), 0.8f)
        add("minecraft:cut_red_sandstone", stack("minecraft:cut_red_sandstone_slab", 9), 0.8f)
        add("minecraft:smooth_red_sandstone", stack("minecraft:smooth_red_sandstone_slab", 9), 0.8f)
        add("minecraft:stone_bricks", stack("minecraft:stone_brick_slab", 9), 1.5f)
        add("minecraft:bricks", stack("minecraft:brick_slab", 9), 2.0f)
        add("minecraft:nether_bricks", stack("minecraft:nether_brick_slab", 9), 2.0f)
        add("minecraft:quartz_block", stack("minecraft:quartz_slab", 9), 0.8f)
        add("minecraft:smooth_quartz", stack("minecraft:smooth_quartz_slab", 9), 0.8f)
        add("minecraft:red_nether_bricks", stack("minecraft:red_nether_brick_slab", 9), 2.0f)
        add("minecraft:purpur_block", stack("minecraft:purpur_slab", 9), 1.5f)
        add("minecraft:prismarine", stack("minecraft:prismarine_slab", 9), 1.5f)
        add("minecraft:dark_prismarine", stack("minecraft:dark_prismarine_slab", 9), 1.5f)
        add("minecraft:prismarine_bricks", stack("minecraft:prismarine_brick_slab", 9), 1.5f)
        add("minecraft:granite", stack("minecraft:granite_slab", 9), 1.5f)
        add("minecraft:polished_granite", stack("minecraft:polished_granite_slab", 9), 1.5f)
        add("minecraft:diorite", stack("minecraft:diorite_slab", 9), 1.5f)
        add("minecraft:polished_diorite", stack("minecraft:polished_diorite_slab", 9), 1.5f)
        add("minecraft:andesite", stack("minecraft:andesite_slab", 9), 1.5f)
        add("minecraft:polished_andesite", stack("minecraft:polished_andesite_slab", 9), 1.5f)
        add("minecraft:blackstone", stack("minecraft:blackstone_slab", 9), 1.5f)
        add("minecraft:polished_blackstone", stack("minecraft:polished_blackstone_slab", 9), 1.5f)
        add("minecraft:polished_blackstone_bricks", stack("minecraft:polished_blackstone_brick_slab", 9), 1.5f)
        add("minecraft:deepslate", stack("minecraft:deepslate_slab", 9), 3.0f)
        add("minecraft:polished_deepslate", stack("minecraft:polished_deepslate_slab", 9), 3.0f)
        add("minecraft:deepslate_bricks", stack("minecraft:deepslate_brick_slab", 9), 3.0f)
        add("minecraft:deepslate_tiles", stack("minecraft:deepslate_tile_slab", 9), 3.0f)
        add("minecraft:mud_bricks", stack("minecraft:mud_brick_slab", 9), 1.5f)
        add("minecraft:oxidized_copper", stack("minecraft:oxidized_cut_copper_slab", 9), 3.0f)
        add("minecraft:weathered_copper", stack("minecraft:weathered_cut_copper_slab", 9), 3.0f)
        add("minecraft:exposed_copper", stack("minecraft:exposed_cut_copper_slab", 9), 3.0f)
        add("minecraft:cut_copper", stack("minecraft:cut_copper_slab", 9), 3.0f)
        add("minecraft:waxed_copper_block", stack("minecraft:waxed_cut_copper_slab", 9), 3.0f)
        add("minecraft:waxed_weathered_copper", stack("minecraft:waxed_weathered_cut_copper_slab", 9), 3.0f)
        add("minecraft:waxed_exposed_copper", stack("minecraft:waxed_exposed_cut_copper_slab", 9), 3.0f)
        add("minecraft:waxed_oxidized_copper", stack("minecraft:waxed_oxidized_cut_copper_slab", 9), 3.0f)

        // 木板类 → 9 台阶
        add("minecraft:oak_planks", stack("minecraft:oak_slab", 9), 2.0f)
        add("minecraft:spruce_planks", stack("minecraft:spruce_slab", 9), 2.0f)
        add("minecraft:birch_planks", stack("minecraft:birch_slab", 9), 2.0f)
        add("minecraft:jungle_planks", stack("minecraft:jungle_slab", 9), 2.0f)
        add("minecraft:acacia_planks", stack("minecraft:acacia_slab", 9), 2.0f)
        add("minecraft:dark_oak_planks", stack("minecraft:dark_oak_slab", 9), 2.0f)
        add("minecraft:mangrove_planks", stack("minecraft:mangrove_slab", 9), 2.0f)
        add("minecraft:cherry_planks", stack("minecraft:cherry_slab", 9), 2.0f)
        add("minecraft:bamboo_planks", stack("minecraft:bamboo_slab", 9), 2.0f)
        add("minecraft:crimson_planks", stack("minecraft:crimson_slab", 9), 2.0f)
        add("minecraft:warped_planks", stack("minecraft:warped_slab", 9), 2.0f)
        add("ic2_120:rubber_planks", stack("ic2_120:rubber_slab", 9), 2.0f)

        // 原木 → 6 木板（增产 50%）
        add("minecraft:oak_log", stack("minecraft:oak_planks", 6), 2.0f)
        add("minecraft:spruce_log", stack("minecraft:spruce_planks", 6), 2.0f)
        add("minecraft:birch_log", stack("minecraft:birch_planks", 6), 2.0f)
        add("minecraft:jungle_log", stack("minecraft:jungle_planks", 6), 2.0f)
        add("minecraft:acacia_log", stack("minecraft:acacia_planks", 6), 2.0f)
        add("minecraft:dark_oak_log", stack("minecraft:dark_oak_planks", 6), 2.0f)
        add("minecraft:mangrove_log", stack("minecraft:mangrove_planks", 6), 2.0f)
        add("minecraft:cherry_log", stack("minecraft:cherry_planks", 6), 2.0f)
        add("minecraft:crimson_stem", stack("minecraft:crimson_planks", 6), 2.0f)
        add("minecraft:warped_stem", stack("minecraft:warped_planks", 6), 2.0f)
        add("ic2_120:rubber_log", stack("ic2_120:rubber_planks", 6), 2.0f)
        add("minecraft:stripped_oak_log", stack("minecraft:oak_planks", 6), 2.0f)
        add("minecraft:stripped_spruce_log", stack("minecraft:spruce_planks", 6), 2.0f)
        add("minecraft:stripped_birch_log", stack("minecraft:birch_planks", 6), 2.0f)
        add("minecraft:stripped_jungle_log", stack("minecraft:jungle_planks", 6), 2.0f)
        add("minecraft:stripped_acacia_log", stack("minecraft:acacia_planks", 6), 2.0f)
        add("minecraft:stripped_dark_oak_log", stack("minecraft:dark_oak_planks", 6), 2.0f)
        add("minecraft:stripped_mangrove_log", stack("minecraft:mangrove_planks", 6), 2.0f)
        add("minecraft:stripped_cherry_log", stack("minecraft:cherry_planks", 6), 2.0f)
        add("minecraft:stripped_crimson_stem", stack("minecraft:crimson_planks", 6), 2.0f)
        add("minecraft:stripped_warped_stem", stack("minecraft:warped_planks", 6), 2.0f)
        add("ic2_120:stripped_rubber_log", stack("ic2_120:rubber_planks", 6), 2.0f)
        add("minecraft:bamboo_block", stack("minecraft:bamboo_planks", 6), 2.0f)
        add("minecraft:stripped_bamboo_block", stack("minecraft:bamboo_planks", 6), 2.0f)

        // 木板 → 6 木棍（2 木板，增产 50%）
        add("minecraft:oak_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "oak_planks_sticks")
        add("minecraft:spruce_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "spruce_planks_sticks")
        add("minecraft:birch_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "birch_planks_sticks")
        add("minecraft:jungle_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "jungle_planks_sticks")
        add("minecraft:acacia_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "acacia_planks_sticks")
        add("minecraft:dark_oak_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "dark_oak_planks_sticks")
        add("minecraft:mangrove_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "mangrove_planks_sticks")
        add("minecraft:cherry_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "cherry_planks_sticks")
        add("minecraft:bamboo_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "bamboo_planks_sticks")
        add("minecraft:crimson_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "crimson_planks_sticks")
        add("minecraft:warped_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "warped_planks_sticks")
        add("ic2_120:rubber_planks", stack("minecraft:stick", 6), 2.0f, overrideKey = "rubber_planks_sticks")
    }

    private fun add(inputId: String, output: ItemStack, materialHardness: Float, overrideKey: String? = null) {
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (output.isEmpty) return
        val key = overrideKey ?: inputId
        // 木板同时有 slab 和 stick 配方，用 overrideKey 区分
        if (overrideKey != null) {
            stickRecipes[item] = Recipe(output, materialHardness)
        } else {
            cache[item] = Recipe(output, materialHardness)
        }
    }

    /**
     * 获取配方。若输入匹配且锯片硬度足够则返回 (输出, 消耗数量)，否则 null。
     * 木板在数量>=2时优先匹配 stick 配方（2木板→6木棍），否则匹配 slab 配方（1木板→9台阶）。
     */
    fun getRecipe(input: ItemStack, bladeHardness: Float): Pair<ItemStack, Int>? {
        if (input.isEmpty) return null
        val item = input.item
        val stickRecipe = stickRecipes[item]
        if (stickRecipe != null && stickRecipe.materialHardness < bladeHardness && input.count >= 2) {
            return stickRecipe.output.copy() to 2
        }
        val recipe = cache[item] ?: return null
        if (recipe.materialHardness >= bladeHardness) return null
        return recipe.output.copy() to 1
    }

    /** 获取材料硬度（用于锯片检查） */
    fun getMaterialHardness(input: ItemStack): Float {
        if (input.isEmpty) return 0f
        return cache[input.item]?.materialHardness ?: stickRecipes[input.item]?.materialHardness ?: run {
            val block = Block.getBlockFromItem(input.item)
            if (block != net.minecraft.block.Blocks.AIR) block.hardness else 2.0f
        }
    }
}

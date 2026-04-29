package ic2_120.config

/**
 * UU 物质复制默认白名单
 *
 * 定价原则：
 * - 有机物 >> 无机物（创造"生命"比创造"死物"更昂贵）
 * - IC2 模组物品保持原值
 * - 原版物品按有机/无机重新平衡
 */
object UuReplicationDefaults {

    val defaultWhitelist: Map<String, Int> = linkedMapOf(
        // ========== 基础无机建材 ==========
        "minecraft:cobblestone" to 10,
        "minecraft:stone" to 20,
        "minecraft:dirt" to 15,
        "minecraft:coarse_dirt" to 20,
        "minecraft:sand" to 15,
        "minecraft:red_sand" to 20,
        "minecraft:gravel" to 12,
        "minecraft:clay_ball" to 20,
        "minecraft:andesite" to 25,
        "minecraft:granite" to 25,
        "minecraft:diorite" to 25,
        "minecraft:polished_andesite" to 50,
        "minecraft:polished_granite" to 50,
        "minecraft:polished_diorite" to 50,

        // ========== 石材加工品 ==========
        "minecraft:sandstone" to 30,
        "minecraft:smooth_sandstone" to 50,
        "minecraft:cut_sandstone" to 50,
        "minecraft:chiseled_sandstone" to 80,
        "minecraft:red_sandstone" to 40,
        "minecraft:sandstone_stairs" to 60,
        "minecraft:sandstone_slab" to 20,

        "minecraft:stone_bricks" to 50,
        "minecraft:mossy_cobblestone" to 50,
        "minecraft:cracked_stone_bricks" to 60,
        "minecraft:stone_slab" to 40,
        "minecraft:stone_pressure_plate" to 60,

        // ========== 陶瓦系列 ==========
        "minecraft:terracotta" to 80,
        "minecraft:white_terracotta" to 100,
        "minecraft:orange_terracotta" to 100,
        "minecraft:yellow_terracotta" to 100,
        "minecraft:light_gray_terracotta" to 100,
        "minecraft:brown_terracotta" to 100,
        "minecraft:red_terracotta" to 100,
        "minecraft:blue_terracotta" to 100,

        // ========== 新版本石头 ==========
        "minecraft:deepslate" to 30,
        "minecraft:polished_deepslate" to 60,
        "minecraft:deepslate_bricks" to 60,
        "minecraft:cracked_deepslate_bricks" to 70,
        "minecraft:deepslate_tiles" to 70,
        "minecraft:calcite" to 20,
        "minecraft:tuff" to 25,
        "minecraft:polished_tuff" to 50,
        "minecraft:mud" to 30,
        "minecraft:mud_bricks" to 60,
        "minecraft:packed_mud" to 50,

        // ========== 玻璃 ==========
        "minecraft:glass" to 100,

        // ========== 矿物系列（保持便宜）==========
        "minecraft:coal" to 500,
        "minecraft:iron_ore" to 800,
        "minecraft:gold_ore" to 5000,
        "minecraft:diamond" to 50000,
        "minecraft:emerald" to 200000,
        "minecraft:lapis_lazuli" to 5000,
        "minecraft:redstone" to 1000,
        "minecraft:flint" to 5000,
        "minecraft:snowball" to 2000,

        // IC2 矿物（保持不变）
        "ic2_120:tin_ore" to 1360,
        "ic2_120:lead_ore" to 9182,
        "ic2_120:uranium_ore" to 16070,
        "ic2_120:iridium_ore_item" to 120000,
        "ic2_120:iridium_shard" to 13330,

        // ========== 金属锭（保持矿石便宜→成品贵的设计）==========
        "minecraft:iron_ingot" to 5000,
        "minecraft:gold_ingot" to 25000,
        "minecraft:gold_block" to 225000,
        "minecraft:copper_ingot" to 1000,

        // ========== 海洋物品 ==========
        "minecraft:prismarine" to 200000,
        "minecraft:prismarine_bricks" to 200000,
        "minecraft:dark_prismarine" to 300000,
        "minecraft:prismarine_crystals" to 300000,
        "minecraft:wet_sponge" to 500000,
        "minecraft:obsidian" to 100000,

        // ========== 有机物-树木（昂贵）==========
        "minecraft:oak_log" to 50000,
        "minecraft:spruce_log" to 50000,
        "minecraft:birch_log" to 50000,
        "minecraft:jungle_log" to 50000,
        "minecraft:acacia_log" to 500000,
        "minecraft:dark_oak_log" to 50000,

        // IC2 橡胶树（保持不变）
        "ic2_120:rubber_sapling" to 3571823,
        "ic2_120:rubber_wood" to 930362,
        "ic2_120:rubber_log" to 930362,
        "ic2_120:resin" to 33314116,

        // 树苗（极贵 - 生命潜力）
        "minecraft:oak_sapling" to 500000,
        "minecraft:spruce_sapling" to 500000,
        "minecraft:birch_sapling" to 500000,
        "minecraft:jungle_sapling" to 500000,
        "minecraft:acacia_sapling" to 1275651,
        "minecraft:dark_oak_sapling" to 500000,

        // 树叶（贵）
        "minecraft:oak_leaves" to 30000,
        "minecraft:spruce_leaves" to 30000,
        "minecraft:birch_leaves" to 30000,
        "minecraft:jungle_leaves" to 30000,
        "minecraft:acacia_leaves" to 30000,
        "minecraft:dark_oak_leaves" to 30000,

        // 木制品（失去生命属性，便宜）
        "minecraft:oak_planks" to 100,
        "minecraft:spruce_planks" to 100,
        "minecraft:birch_planks" to 100,
        "minecraft:jungle_planks" to 100,
        "minecraft:acacia_planks" to 100,
        "minecraft:dark_oak_planks" to 100,

        "minecraft:oak_fence" to 150,
        "minecraft:oak_stairs" to 200,
        "minecraft:oak_door" to 200,
        "minecraft:oak_slab" to 60,
        "minecraft:oak_fence_gate" to 250,
        "minecraft:oak_trapdoor" to 250,

        // ========== 有机物-农作物（昂贵）==========
        "minecraft:wheat_seeds" to 200000,
        "minecraft:melon_seeds" to 200000,
        "minecraft:pumpkin_seeds" to 200000,
        "minecraft:beetroot_seeds" to 200000,
        "minecraft:cocoa_beans" to 200000,

        "minecraft:wheat" to 500000,
        "minecraft:carrot" to 500000,
        "minecraft:potato" to 500000,
        "minecraft:beetroot" to 500000,
        "minecraft:melon" to 500000,
        "minecraft:pumpkin" to 500000,

        "minecraft:cactus" to 300000,
        "minecraft:sugar_cane" to 500000,
        "minecraft:lily_pad" to 200000,
        "minecraft:vine" to 200000,

        // ========== 有机物-花卉（贵）==========
        "minecraft:dandelion" to 150000,
        "minecraft:poppy" to 150000,
        "minecraft:blue_orchid" to 150000,
        "minecraft:allium" to 150000,
        "minecraft:azure_bluet" to 150000,
        "minecraft:red_tulip" to 150000,
        "minecraft:orange_tulip" to 150000,
        "minecraft:white_tulip" to 150000,
        "minecraft:pink_tulip" to 150000,
        "minecraft:oxeye_daisy" to 150000,
        "minecraft:sunflower" to 150000,
        "minecraft:lilac" to 150000,
        "minecraft:rose_bush" to 150000,
        "minecraft:peony" to 150000,

        // ========== 有机物-食物（极贵）==========
        "minecraft:apple" to 800000,
        "minecraft:bread" to 1000000,
        "minecraft:cookie" to 500000,
        "minecraft:cake" to 2000000,
        "minecraft:golden_apple" to 5000000,
        "minecraft:enchanted_golden_apple" to 50000000,
        "minecraft:brown_mushroom" to 400000,
        "minecraft:red_mushroom" to 400000,

        // ========== 有机物-生物掉落 ==========
        "minecraft:bone" to 200000,
        "minecraft:rotten_flesh" to 200000,
        "minecraft:spider_eye" to 100000,
        "minecraft:string" to 50000,
        "minecraft:leather" to 300000,
        "minecraft:feather" to 300000,
        "minecraft:ender_pearl" to 500000,
        "minecraft:slime_ball" to 500000,
        "minecraft:gunpowder" to 50000,

        // ========== 羊毛 ==========
        "minecraft:black_wool" to 200000,

        // ========== 其他有机物 ==========
        "minecraft:stick" to 500000,
        "minecraft:torch" to 100000,

        // ========== 功能方块 ==========
        "minecraft:crafting_table" to 100,
        "minecraft:chest" to 200,
        "minecraft:furnace" to 150,
        "minecraft:brewing_stand" to 1000,
        "minecraft:bookshelf" to 500,
        "minecraft:ladder" to 80,
        "minecraft:rail" to 50000,

        // ========== 特殊物品 ==========
        "minecraft:book" to 500000,
        "minecraft:bucket" to 200,
        "minecraft:tnt" to 10000,
        "minecraft:name_tag" to 25000000,
        "minecraft:saddle" to 27000000,
        "minecraft:enchanted_book" to 100000000,
        "minecraft:iron_horse_armor" to 100000,
        "minecraft:golden_horse_armor" to 250000,
        "minecraft:diamond_horse_armor" to 500000,
        "minecraft:music_disc_13" to 35000000,
        "minecraft:music_disc_cat" to 39000000
    )
}

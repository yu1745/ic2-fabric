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
 * 配方按 IC2 提取机标准设计。
 */
object ExtractorRecipes {

    private val cache = mutableMapOf<Item, ItemStack>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            val item = Registries.ITEM.get(ident)
            return if (item != Items.AIR) ItemStack(item, count) else ItemStack.EMPTY
        }

        // === 图片配方：基础材料 ===
        add("minecraft:gravel", ItemStack(Items.FLINT, 1))                      // 沙砾 -> 燧石
        add("minecraft:spider_eye", stack("ic2_120:grin_powder", 2))             // 蜘蛛眼 -> 2 蛤蛤粉
        add("minecraft:redstone_ore", ItemStack(Items.REDSTONE, 6))              // 红石矿石 -> 6 红石
        add("minecraft:deepslate_redstone_ore", ItemStack(Items.REDSTONE, 6))    // 深板岩红石矿石 -> 6 红石
        add("minecraft:diamond_ore", ItemStack(Items.DIAMOND, 2))                // 钻石矿石 -> 2 钻石
        add("minecraft:deepslate_diamond_ore", ItemStack(Items.DIAMOND, 2))      // 深板岩钻石矿石 -> 2 钻石
        add("minecraft:lapis_ore", ItemStack(Items.LAPIS_LAZULI, 6))             // 青金石矿石 -> 6 青金石
        add("minecraft:deepslate_lapis_ore", ItemStack(Items.LAPIS_LAZULI, 6))   // 深板岩青金石矿石 -> 6 青金石
        add("minecraft:clay", stack("ic2_120:clay_dust", 2))                    // 黏土块 -> 2 粘土粉
        add("ic2_120:coffee_beans", stack("ic2_120:coffee_powder", 3))         // 咖啡豆 -> 3 咖啡粉
        add("minecraft:cobblestone", ItemStack(Items.SAND, 1))                   // 圆石 -> 沙子
        add("minecraft:stone", ItemStack(Items.COBBLESTONE, 1))                 // 石头 -> 圆石
        add("minecraft:granite", ItemStack(Items.COBBLESTONE, 1))                 // 花岗岩 -> 圆石
        add("minecraft:diorite", ItemStack(Items.COBBLESTONE, 1))                 // 闪长岩 -> 圆石
        add("minecraft:andesite", ItemStack(Items.COBBLESTONE, 1))               // 安山岩 -> 圆石
        add("minecraft:poisonous_potato", stack("ic2_120:grin_powder", 1))     // 毒马铃薯 -> 蛤蛤粉
        add("minecraft:ice", ItemStack(Items.SNOWBALL, 1))                      // 冰 -> 雪球
        add("minecraft:packed_ice", ItemStack(Items.SNOWBALL, 1))               // 浮冰 -> 雪球
        add("minecraft:sandstone", ItemStack(Items.SAND, 1))                    // 沙石 -> 沙子
        add("minecraft:cut_sandstone", ItemStack(Items.SAND, 1))                 // 切制沙石 -> 沙子
        add("minecraft:chiseled_sandstone", ItemStack(Items.SAND, 1))           // 錾制沙石 -> 沙子
        add("minecraft:smooth_sandstone", ItemStack(Items.SAND, 1))             // 平滑沙石 -> 沙子
        add("minecraft:red_sandstone", ItemStack(Items.RED_SAND, 1))            // 红沙石 -> 红沙
        add("minecraft:cut_red_sandstone", ItemStack(Items.RED_SAND, 1))        // 切制红沙石 -> 红沙
        add("minecraft:chiseled_red_sandstone", ItemStack(Items.RED_SAND, 1))   // 錾制红沙石 -> 红沙
        add("minecraft:smooth_red_sandstone", ItemStack(Items.RED_SAND, 1))    // 平滑红沙石 -> 红沙
        add("minecraft:ender_pearl", stack("ic2_120:ender_pearl_dust", 1))    // 末影珍珠 -> 末影珍珠粉
        add("minecraft:wheat", stack("ic2_120:flour", 1))                      // 小麦 -> 面粉
        add("minecraft:blaze_rod", ItemStack(Items.BLAZE_POWDER, 5))           // 烈焰棒 -> 5 烈焰粉
        add("minecraft:white_wool", ItemStack(Items.STRING, 2))                 // 白色羊毛 -> 2 线
        add("minecraft:orange_wool", ItemStack(Items.STRING, 2))                // 橙色羊毛 -> 2 线
        add("minecraft:magenta_wool", ItemStack(Items.STRING, 2))                // 品红色羊毛 -> 2 线
        add("minecraft:light_blue_wool", ItemStack(Items.STRING, 2))            // 浅蓝色羊毛 -> 2 线
        add("minecraft:yellow_wool", ItemStack(Items.STRING, 2))                // 黄色羊毛 -> 2 线
        add("minecraft:lime_wool", ItemStack(Items.STRING, 2))                  // 柠檬色羊毛 -> 2 线
        add("minecraft:pink_wool", ItemStack(Items.STRING, 2))                 // 粉色羊毛 -> 2 线
        add("minecraft:gray_wool", ItemStack(Items.STRING, 2))                  // 灰色羊毛 -> 2 线
        add("minecraft:light_gray_wool", ItemStack(Items.STRING, 2))           // 浅灰色羊毛 -> 2 线
        add("minecraft:cyan_wool", ItemStack(Items.STRING, 2))                  // 青色羊毛 -> 2 线
        add("minecraft:purple_wool", ItemStack(Items.STRING, 2))                // 紫色羊毛 -> 2 线
        add("minecraft:blue_wool", ItemStack(Items.STRING, 2))                  // 蓝色羊毛 -> 2 线
        add("minecraft:brown_wool", ItemStack(Items.STRING, 2))                 // 棕色羊毛 -> 2 线
        add("minecraft:green_wool", ItemStack(Items.STRING, 2))                 // 绿色羊毛 -> 2 线
        add("minecraft:red_wool", ItemStack(Items.STRING, 2))                    // 红色羊毛 -> 2 线
        add("minecraft:black_wool", ItemStack(Items.STRING, 2))                 // 黑色羊毛 -> 2 线
        add("minecraft:bone", ItemStack(Items.BONE_MEAL, 5))                    // 骨头 -> 5 骨粉
        add("minecraft:glowstone", ItemStack(Items.GLOWSTONE_DUST, 4))          // 萤石 -> 4 萤石粉
        add("ic2_120:plant_ball", ItemStack(Items.DIRT, 1))                     // 植物球 -> 泥土
        add("ic2_120:energy_crystal", stack("ic2_120:energium_dust", 9))        // 能量水晶 -> 9 能量水晶粉

        // === 矿石 (铁/金/铜/锡) -> 2 粉碎矿石 ===
        add("minecraft:iron_ore", stack("ic2_120:crushed_iron", 2))            // 铁矿石 -> 2 粉碎铁
        add("minecraft:gold_ore", stack("ic2_120:crushed_gold", 2))             // 金矿石 -> 2 粉碎金
        add("minecraft:copper_ore", stack("ic2_120:crushed_copper", 2))        // 原版铜矿石 -> 2 粉碎铜
        add("ic2_120:tin_ore", stack("ic2_120:crushed_tin", 2))                // 锡矿石 -> 2 粉碎锡
        add("minecraft:deepslate_iron_ore", stack("ic2_120:crushed_iron", 2))  // 深板岩铁矿石 -> 2 粉碎铁
        add("minecraft:deepslate_gold_ore", stack("ic2_120:crushed_gold", 2))  // 深板岩金矿石 -> 2 粉碎金
        add("minecraft:deepslate_copper_ore", stack("ic2_120:crushed_copper", 2)) // 深板岩铜矿石 -> 2 粉碎铜
        add("ic2_120:deepslate_tin_ore", stack("ic2_120:crushed_tin", 2))     // 深板岩锡矿石 -> 2 粉碎锡

        // === 矿锭/板 -> 粉末 ===
        add("minecraft:iron_ingot", stack("ic2_120:iron_dust", 1))              // 铁锭 -> 铁粉
        add("minecraft:gold_ingot", stack("ic2_120:gold_dust", 1))             // 金锭 -> 金粉
        add("minecraft:copper_ingot", stack("ic2_120:copper_dust", 1))         // 原版铜锭 -> 铜粉
        add("ic2_120:tin_ingot", stack("ic2_120:tin_dust", 1))                // 锡锭 -> 锡粉
        add("ic2_120:bronze_ingot", stack("ic2_120:bronze_dust", 1))           // 青铜锭 -> 青铜粉
        add("ic2_120:lead_ingot", stack("ic2_120:lead_dust", 1))              // 铅锭 -> 铅粉
        add("ic2_120:silver_ingot", stack("ic2_120:silver_dust", 1))          // 银锭 -> 银粉
        add("ic2_120:steel_ingot", stack("ic2_120:iron_dust", 1))             // 钢锭 -> 铁粉（钢含铁）
        add("ic2_120:iron_plate", stack("ic2_120:iron_dust", 1))              // 铁板 -> 铁粉
        add("ic2_120:gold_plate", stack("ic2_120:gold_dust", 1))              // 金板 -> 金粉
        add("ic2_120:copper_plate", stack("ic2_120:copper_dust", 1))            // 铜板 -> 铜粉
        add("ic2_120:tin_plate", stack("ic2_120:tin_dust", 1))                // 锡板 -> 锡粉
        add("ic2_120:bronze_plate", stack("ic2_120:bronze_dust", 1))            // 青铜板 -> 青铜粉
        add("ic2_120:lead_plate", stack("ic2_120:lead_dust", 1))               // 铅板 -> 铅粉

        // === 矿石块 -> 9 粉末 ===
        add("minecraft:iron_block", stack("ic2_120:iron_dust", 9))             // 铁块 -> 9 铁粉
        add("minecraft:gold_block", stack("ic2_120:gold_dust", 9))             // 金块 -> 9 金粉
        add("minecraft:copper_block", stack("ic2_120:copper_dust", 9))         // 原版铜块 -> 9 铜粉
        add("ic2_120:tin_block", stack("ic2_120:tin_dust", 9))                 // 锡块 -> 9 锡粉
        add("ic2_120:bronze_block", stack("ic2_120:bronze_dust", 9))           // 青铜块 -> 9 青铜粉
        add("ic2_120:lead_block", stack("ic2_120:lead_dust", 9))                // 铅块 -> 9 铅粉
        add("ic2_120:silver_block", stack("ic2_120:silver_dust", 9))           // 银块 -> 9 银粉

        // === 石英加工品 -> 下界石英 ===
        add("minecraft:quartz_block", ItemStack(Items.QUARTZ, 4))               // 石英块 -> 4 下界石英
        add("minecraft:chiseled_quartz_block", ItemStack(Items.QUARTZ, 4))     // 錾制石英块 -> 4 下界石英
        add("minecraft:quartz_pillar", ItemStack(Items.QUARTZ, 4))              // 石英柱 -> 4 下界石英
        add("minecraft:smooth_quartz", ItemStack(Items.QUARTZ, 4))               // 平滑石英 -> 4 下界石英
        add("minecraft:quartz_slab", ItemStack(Items.QUARTZ, 2))                 // 石英台阶 -> 2 下界石英
        add("minecraft:smooth_quartz_slab", ItemStack(Items.QUARTZ, 2))         // 平滑石英台阶 -> 2 下界石英
        add("minecraft:quartz_stairs", ItemStack(Items.QUARTZ, 4))              // 石英楼梯 -> 4 下界石英
        add("minecraft:smooth_quartz_stairs", ItemStack(Items.QUARTZ, 4))       // 平滑石英楼梯 -> 4 下界石英
        add("minecraft:nether_quartz_ore", ItemStack(Items.QUARTZ, 2))           // 下界石英矿石 -> 2 下界石英

        // === 橡胶类（保留原有） ===
        add("ic2_120:resin", stack("ic2_120:rubber", 3))                       // 粘性树脂 -> 3 橡胶
        add("ic2_120:rubber_log", stack("ic2_120:rubber", 1))                  // 橡胶原木 -> 1 橡胶
        add("ic2_120:stripped_rubber_log", stack("ic2_120:rubber", 1))          // 去皮橡胶原木 -> 1 橡胶
        add("ic2_120:rubber_wood", stack("ic2_120:rubber", 1))                 // 橡胶木 -> 1 橡胶
        add("ic2_120:stripped_rubber_wood", stack("ic2_120:rubber", 1))         // 去皮橡胶木 -> 1 橡胶
        add("ic2_120:rubber_sapling", stack("ic2_120:rubber", 1))              // 橡胶树苗 -> 1 橡胶
        addOptional("techreborn:rubber_log", stack("ic2_120:rubber", 1))       // TechReborn 橡胶原木 -> 1 橡胶（可选）
        addOptional("techreborn:rubber_log_stripped", stack("ic2_120:rubber", 1)) // TechReborn 去皮橡胶原木 -> 1 橡胶（可选）

        // === 单元类（保留原有） ===
        add("ic2_120:air_cell", stack("ic2_120:empty_cell", 1))                 // 空气单元 -> 空单元
        add("ic2_120:bio_cell", stack("ic2_120:biofuel_cell", 1))               // 生物单元 -> 生物燃料单元
        add("ic2_120:compressed_hydrated_coal", stack("ic2_120:coal_fuel_dust", 1)) // 压缩水合煤 -> 湿煤粉

        // === 块 -> 4 份材料（保留原有） ===
        add("minecraft:bricks", ItemStack(Items.BRICK, 4))                     // 砖块 -> 4 砖
        add("minecraft:nether_bricks", ItemStack(Items.NETHER_BRICK, 4))        // 下界砖块 -> 4 下界砖
        add("minecraft:snow_block", ItemStack(Items.SNOWBALL, 4))               // 雪块 -> 4 雪球

        // === 其他（保留原有） ===
        add("minecraft:gunpowder", stack("ic2_120:sulfur_dust", 1))             // 火药 -> 硫粉
        add("ic2_120:filled_tin_can", stack("ic2_120:tin_can", 1))              // 装满的锡罐 -> 锡罐
        add("ic2_120:hydrated_tin_dust", stack("ic2_120:iodine", 1))            // 氢氧化锡粉 -> 碘
        add("ic2_120:netherrack_dust", stack("ic2_120:small_sulfur_dust", 1))   // 地狱岩粉 -> 小撮硫粉
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
        if (item != Items.AIR) cache[item] = output
    }

    fun getOutput(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        return cache[input.item]?.copy()
    }
}

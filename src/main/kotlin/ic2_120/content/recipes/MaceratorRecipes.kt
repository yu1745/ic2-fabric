package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 粉碎机配方：输入物品 -> 输出物品及数量。
 * 使用原版与模组物品 ID 匹配。
 */
object MaceratorRecipes {

    private val cache = mutableMapOf<Item, ItemStack>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }
        // 矿石 -> 2 粉碎矿石
        add("minecraft:iron_ore", stack("ic2_120:crushed_iron", 2))           // 铁矿石 -> 2 粉碎铁
        add("minecraft:gold_ore", stack("ic2_120:crushed_gold", 2))            // 金矿石 -> 2 粉碎金
        add("minecraft:copper_ore", stack("ic2_120:crushed_copper", 2))        // 原版铜矿石 -> 2 粉碎铜
        add("ic2_120:lead_ore", stack("ic2_120:crushed_lead", 2))             // 铅矿石 -> 2 粉碎铅
        add("ic2_120:tin_ore", stack("ic2_120:crushed_tin", 2))               // 锡矿石 -> 2 粉碎锡
        add("ic2_120:uranium_ore", stack("ic2_120:crushed_uranium", 2))       // 铀矿石 -> 2 粉碎铀
        add("ic2_120:silver_ore", stack("ic2_120:crushed_silver", 2))         // 银矿石 -> 2 粉碎银
        add("minecraft:deepslate_iron_ore", stack("ic2_120:crushed_iron", 2)) // 深板岩铁矿石 -> 2 粉碎铁
        add("minecraft:deepslate_gold_ore", stack("ic2_120:crushed_gold", 2))  // 深板岩金矿石 -> 2 粉碎金
        add("minecraft:deepslate_copper_ore", stack("ic2_120:crushed_copper", 2)) // 深板岩铜矿石 -> 2 粉碎铜
        add("ic2_120:deepslate_lead_ore", stack("ic2_120:crushed_lead", 2))    // 深板岩铅矿石 -> 2 粉碎铅
        add("ic2_120:deepslate_tin_ore", stack("ic2_120:crushed_tin", 2))     // 深板岩锡矿石 -> 2 粉碎锡
        add("ic2_120:deepslate_uranium_ore", stack("ic2_120:crushed_uranium", 2)) // 深板岩铀矿石 -> 2 粉碎铀
        // 石头类 -> 圆石/沙砾/燧石
        add("minecraft:cobblestone", ItemStack(net.minecraft.item.Items.GRAVEL, 1))   // 圆石 -> 1 沙砾
        add("minecraft:gravel", ItemStack(net.minecraft.item.Items.FLINT, 1))         // 沙砾 -> 1 燧石
        add("minecraft:stone", ItemStack(net.minecraft.item.Items.COBBLESTONE, 1))    // 石头 -> 1 圆石
        add("minecraft:granite", ItemStack(net.minecraft.item.Items.COBBLESTONE, 1)) // 花岗岩 -> 1 圆石
        add("minecraft:diorite", ItemStack(net.minecraft.item.Items.COBBLESTONE, 1)) // 闪长岩 -> 1 圆石
        add("minecraft:andesite", ItemStack(net.minecraft.item.Items.COBBLESTONE, 1)) // 安山岩 -> 1 圆石
        add("minecraft:netherrack", stack("ic2_120:netherrack_dust", 1))             // 地狱岩 -> 1 地狱岩粉
    }

    private fun add(inputId: String, output: ItemStack) {
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (output.isEmpty) return
        cache[item] = output
    }

    fun getOutput(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        return cache[input.item]?.copy()
    }
}

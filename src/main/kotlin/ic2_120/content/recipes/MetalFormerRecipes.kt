package ic2_120.content.recipes

import ic2_120.content.sync.MetalFormerSync
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 金属成型机配方：输入物品 -> 输出物品及数量。
 * 支持三种加工模式：辊压、切割、挤压。
 */
object MetalFormerRecipes {

    private val cache = mutableMapOf<ModeKey, ItemStack>()

    data class ModeKey(
        val mode: MetalFormerSync.Mode,
        val inputItem: Item,
        val secondaryItem: Item? = null
    ) {
        override fun hashCode(): Int {
            var result = mode.hashCode()
            result = 31 * result + inputItem.hashCode()
            result = 31 * result + (secondaryItem?.hashCode() ?: 0)
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ModeKey) return false
            return mode == other.mode &&
                   inputItem == other.inputItem &&
                   secondaryItem == other.secondaryItem
        }
    }

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }

        fun item(id: String): Item? {
            val ident = Identifier.tryParse(id) ?: return null
            return Registries.ITEM.get(ident)
        }

        // ========== 辊压模式 (Rolling)：锭压成板，板压成外壳 ==========
        val rolling = MetalFormerSync.Mode.ROLLING

        // 锭 -> 板
        add(rolling, "ic2_120:tin_ingot", stack("ic2_120:tin_plate", 1))       // 锡锭 -> 锡板
        add(rolling, "minecraft:iron_ingot", stack("ic2_120:iron_plate", 1))     // 铁锭 -> 铁板
        add(rolling, "minecraft:copper_ingot", stack("ic2_120:copper_plate", 1)) // 原版铜锭 -> 铜板
        add(rolling, "ic2_120:bronze_ingot", stack("ic2_120:bronze_plate", 1))// 青铜锭 -> 青铜板
        add(rolling, "minecraft:gold_ingot", stack("ic2_120:gold_plate", 1))    // 金锭 -> 金板
        add(rolling, "ic2_120:lead_ingot", stack("ic2_120:lead_plate", 1))      // 铅锭 -> 铅板
        add(rolling, "ic2_120:steel_ingot", stack("ic2_120:steel_plate", 1))   // 钢锭 -> 钢板

        // 板 -> 2x 外壳
        add(rolling, "ic2_120:tin_plate", stack("ic2_120:tin_casing", 2))      // 锡板 -> 2*锡质外壳
        add(rolling, "ic2_120:iron_plate", stack("ic2_120:iron_casing", 2))    // 铁板 -> 2*铁质外壳
        add(rolling, "ic2_120:copper_plate", stack("ic2_120:copper_casing", 2))// 铜板 -> 2*铜质外壳
        add(rolling, "ic2_120:bronze_plate", stack("ic2_120:bronze_casing", 2))// 青铜板 -> 2*青铜外壳
        add(rolling, "ic2_120:gold_plate", stack("ic2_120:gold_casing", 2))   // 金板 -> 2*黄金外壳
        add(rolling, "ic2_120:lead_plate", stack("ic2_120:lead_casing", 2))   // 铅板 -> 2*铅质外壳
        add(rolling, "ic2_120:steel_plate", stack("ic2_120:steel_casing", 2)) // 钢板 -> 2*钢质外壳

        // ========== 切割模式 (Cutting)：板材/外壳切割成导线或货币 ==========
        val cutting = MetalFormerSync.Mode.CUTTING

        // 板 -> 导线（高压导线即 iron_cable）
        add(cutting, "ic2_120:tin_plate", stack("ic2_120:tin_cable", 3))        // 锡板 -> 3*锡质导线
        add(cutting, "ic2_120:copper_plate", stack("ic2_120:copper_cable", 2)) // 铜板 -> 2*铜质导线
        add(cutting, "ic2_120:gold_plate", stack("ic2_120:gold_cable", 4))     // 金板 -> 4*金质导线
        add(cutting, "ic2_120:iron_plate", stack("ic2_120:iron_cable", 4))      // 铁板 -> 4*高压导线

        // 外壳 -> 货币
        add(cutting, "ic2_120:iron_casing", stack("ic2_120:coin", 2))           // 铁质外壳 -> 2*工业货币

        // ========== 挤压模式 (Extruding)：锭/外壳/板/块挤压成导线或组件 ==========
        val extruding = MetalFormerSync.Mode.EXTRUDING

        // 锭 -> 导线
        add(extruding, "ic2_120:tin_ingot", stack("ic2_120:tin_cable", 3))      // 锡锭 -> 3*锡质导线
        add(extruding, "minecraft:gold_ingot", stack("ic2_120:gold_cable", 4)) // 金锭 -> 4*金质导线
        add(extruding, "minecraft:copper_ingot", stack("ic2_120:copper_cable", 3))// 原版铜锭 -> 3*铜质导线
        add(extruding, "minecraft:iron_ingot", stack("ic2_120:iron_cable", 4))   // 铁锭 -> 4*高压导线

        // 外壳/板/块 -> 制品（单输入）
        add(extruding, "ic2_120:tin_casing", stack("ic2_120:tin_can", 1))       // 锡质外壳 -> 1*空锡罐
        add(extruding, "ic2_120:iron_plate", stack("ic2_120:fuel_rod", 1))     // 铁板 -> 1*空燃料棒
        add(extruding, "ic2_120:iron_casing", stack("ic2_120:iron_fence", 1))   // 铁质外壳 -> 1*铁栅栏
        add(extruding, "minecraft:iron_block", stack("ic2_120:iron_shaft", 1)) // 铁块 -> 1*铁柄(铁)
        add(extruding, "ic2_120:steel_block", stack("ic2_120:steel_shaft", 1)) // 钢块 -> 1*铁柄(钢)
        // 注：锡板 -> 3*空单元 在 IC2 2.6 版本起已移除，此处不实现
    }

    private fun add(mode: MetalFormerSync.Mode, inputId: String, output: ItemStack) {
        val inputItem = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (output.isEmpty) return
        cache[ModeKey(mode, inputItem)] = output
    }

    private fun add(mode: MetalFormerSync.Mode, inputId: String, secondaryId: String, output: ItemStack) {
        val inputItem = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        val secondaryItem = Registries.ITEM.get(Identifier.tryParse(secondaryId) ?: return)
        if (output.isEmpty) return
        cache[ModeKey(mode, inputItem, secondaryItem)] = output
    }

    fun getOutput(mode: MetalFormerSync.Mode, input: ItemStack, secondary: ItemStack? = null): ItemStack? {
        if (input.isEmpty) return null

        // 先尝试单输入配方
        val singleKey = ModeKey(mode, input.item)
        cache[singleKey]?.let { return it.copy() }

        // 再尝试双输入配方
        if (secondary != null && !secondary.isEmpty) {
            val doubleKey = ModeKey(mode, input.item, secondary.item)
            cache[doubleKey]?.let { return it.copy() }
        }

        return null
    }
}

package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 高炉配方：铁质材料 + 热量 + 空气 → 钢锭 + 炉渣
 *
 * 支持的输入材料：
 * - 铁粉、粉碎铁矿石、纯净的粉碎铁矿石、铁锭、铁矿石、深板岩铁矿石
 */
object BlastFurnaceRecipes {

    private val cache = mutableMapOf<Item, ItemStack>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }

        val steelIngot = stack("ic2_120:steel_ingot", 1)
        val slag = stack("ic2_120:slag", 1)

        add("ic2_120:iron_dust", steelIngot, slag)
        add("ic2_120:crushed_iron", steelIngot, slag)
        add("ic2_120:purified_iron", steelIngot, slag)
        add("minecraft:iron_ingot", steelIngot, slag)
        add("minecraft:iron_ore", steelIngot, slag)
        add("minecraft:deepslate_iron_ore", steelIngot, slag)
    }

    private fun add(inputId: String, steel: ItemStack, slag: ItemStack) {
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (steel.isEmpty || slag.isEmpty) return
        cache[item] = steel
    }

    /** 获取钢锭输出（炉渣固定为 1 个） */
    fun getSteelOutput(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        return cache[input.item]?.copy()
    }

    /** 炉渣输出固定为 1 个 */
    fun getSlagOutput(): ItemStack =
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "slag")), 1)

    fun isValidInput(input: ItemStack): Boolean = getSteelOutput(input) != null
}

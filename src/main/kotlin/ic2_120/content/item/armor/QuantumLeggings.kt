package ic2_120.content.item.armor

import ic2_120.content.item.ModArmorMaterials
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 量子护腿 (Quantum Leggings)
 *
 * 量子套装的护腿部件，提供中等减伤。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：30%
 *
 * ## 待实现功能
 *
 * - 3 倍奔跑速度
 * - 冰上 9 倍速度（双击 W 激活）
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_leggings", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumLeggings : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.LEGGINGS, FabricItemSettings().maxCount(1)) {

    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Text.literal("减伤: 30%").formatted(Formatting.GRAY))
    }
}

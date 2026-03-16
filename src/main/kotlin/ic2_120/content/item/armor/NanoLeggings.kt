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
 * 纳米护腿 (Nano Leggings)
 *
 * 纳米套装的护腿部件，提供中等减伤。
 *
 * ## 核心参数
 *
 * - 载电量：1,000,000 EU（1 MEU）
 * - 能量等级：3
 * - 减伤比例：30%
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有纳米装备均匀扣除
 */
@ModItem(name = "nano_leggings", tab = CreativeTab.IC2_MATERIALS, group = "nano_armor")
class NanoLeggings : NanoArmorItem(ModArmorMaterials.NANO_ARMOR, ArmorItem.Type.LEGGINGS, FabricItemSettings().maxCount(1)) {

    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Text.literal("减伤: 23.07%").formatted(Formatting.GRAY))
    }
}

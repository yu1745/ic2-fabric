package ic2_120.content.item.armor

import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting

/**
 * 纳米靴子 (Nano Boots)
 *
 * 纳米套装的靴子部件，提供基础减伤。
 *
 * ## 核心参数
 *
 * - 载电量：1,000,000 EU（1 MEU）
 * - 能量等级：3
 * - 减伤比例：8%    （量子 10% × 0.8）
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 20000 EU × 配置倍率（默认倍率 1.0）
 * - 能量从所有纳米装备均匀扣除
 */
@ModItem(name = "nano_boots", tab = CreativeTab.IC2_MATERIALS, group = "nano_armor")
class NanoBoots : NanoArmorItem(ModArmorMaterials.NANO_ARMOR, ArmorItem.Type.BOOTS, Item.Properties().stacksTo(1)) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = CarbonPlate::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            if (plate == Items.AIR || crystal == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = NanoBoots::class.id(),
                result = NanoBoots::class.instance(),
                pattern = listOf("   ", "C C", "CEC"),
                keys = mapOf<Char, Item>(
                    'C' to plate,
                    'E' to crystal
                ),
                category = "equipment"
            )
        }
    }

    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, type)
                val pct = "%.2f".format(getDamageReduction() * 100).trimEnd('0').trimEnd('.')
        tooltip.add(Component.translatable("tooltip.ic2_120.armor.damage_reduction", pct).withStyle(ChatFormatting.GRAY))
    }
}

package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.RubberBoots
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.Consumer

/**
 * 量子靴子 (Quantum Boots)
 *
 * 量子套装的靴子部件，提供大跳功能。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：11%
 *
 * ## 大跳功能
 *
 * - Alt + J 开关
 * - 跳起高度 ~3.75 格（3 倍正常跳高）
 * - 耗电：10,000 EU/次（满电约 1000 跳）
 * - 落地自动免疫摔落伤害
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_boots", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumBoots : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.BOOTS, FabricItemSettings().maxCount(1)) {

    companion object {
        private const val SUPER_JUMP_KEY = "SuperJumpEnabled"
        const val SUPER_JUMP_PROTECTION_KEY = "SuperJumpProtection"

        val jumpEnergyCost: Long
            get() = Ic2Config.getQuantumBootsJumpEnergyCost()

        @JvmStatic
        fun isSuperJumpEnabled(stack: ItemStack): Boolean =
            stack.orCreateNbt.getBoolean(SUPER_JUMP_KEY)

        @JvmStatic
        fun toggleSuperJump(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBoolean(SUPER_JUMP_KEY)
            nbt.putBoolean(SUPER_JUMP_KEY, enabled)
            return enabled
        }

        @JvmStatic
        fun getJumpHeightMultiplier(): Double =
            Ic2Config.getQuantumBootsJumpHeightMultiplier()

        /**
         * 消耗大跳能量。由混合在跳跃时调用。
         * @return true 如果消耗成功
         */
        @JvmStatic
        fun consumeJumpEnergy(stack: ItemStack): Boolean {
            val item = stack.item as? QuantumBoots ?: return false
            val energy = item.getEnergy(stack)
            val cost = jumpEnergyCost
            if (energy < cost) return false
            item.setEnergy(stack, energy - cost)
            return true
        }

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val iridium = IridiumPlate::class.instance()
            val nano = NanoBoots::class.instance()
            val rubber = RubberBoots::class.instance()
            val lapotron = LapotronCrystalItem::class.instance()
            if (iridium == Items.AIR || nano == Items.AIR || rubber == Items.AIR || lapotron == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = QuantumBoots::class.id(),
                result = QuantumBoots::class.instance(),
                pattern = listOf("   ", "INI", "RLR"),
                keys = mapOf<Char, Item>(
                    'I' to iridium,
                    'N' to nano,
                    'R' to rubber,
                    'L' to lapotron
                ),
                category = "equipment"
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val jumpEnabled = stack.orCreateNbt.getBoolean(SUPER_JUMP_KEY)
        val energy = getEnergy(stack)
        val jumpCount = if (jumpEnergyCost > 0) energy / jumpEnergyCost else 0L

        tooltip.add(Text.literal("大跳: ").append(
            Text.translatable(if (jumpEnabled) "tooltip.ic2_120.status.on" else "tooltip.ic2_120.status.off")
        ).append(Text.literal(" | 剩余: ${jumpCount}次")).formatted(Formatting.GRAY))
        tooltip.add(Text.literal("减伤: 11% | 落地免伤").formatted(Formatting.GRAY))
    }
}

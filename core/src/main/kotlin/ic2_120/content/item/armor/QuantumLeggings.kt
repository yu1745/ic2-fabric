package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.content.block.MachineCasingBlock
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
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
 * 量子护腿 (Quantum Leggings)
 *
 * 量子套装的护腿部件，提供神行加速功能。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：30%
 *
 * ## 神行功能
 *
 * - Alt + L 三档循环：关 → 1档 → 2档
 * - 1档：原版 IC2 推进力的一半（地面 0.11、水中 0.05），半耗电（约 139 EU/t）
 * - 2档：复刻原版 IC2 推进力（地面 0.22、水中 0.1），全耗电（约 278 EU/t）
 * - 满电 1档可跑 60 分钟，2档可跑 30 分钟
 * - 只有实际向前推进时耗电
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_leggings", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumLeggings : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.LEGGINGS, FabricItemSettings().maxCount(1)) {

    companion object {
        private const val SPEED_TIER_KEY = "SpeedTier"
        private const val SPEED_REMAINDER_KEY = "QuantumLeggingsSpeedRemainder"

        val speedBoostDurationSeconds: Int
            get() = Ic2Config.current.armor.quantumLeggings.speedBoostDurationSeconds

        fun getGroundBoost(tier: Int): Double =
            Ic2Config.getQuantumLeggingsGroundBoost(tier)

        fun getWaterBoost(tier: Int): Double =
            Ic2Config.getQuantumLeggingsWaterBoost(tier)

        /**
         * 循环速度档位：0 → 1 → 2 → 0
         * @return 切换后的档位
         */
        fun cycleSpeedTier(stack: ItemStack): Int {
            val nbt = stack.orCreateNbt
            val current = nbt.getInt(SPEED_TIER_KEY)
            val next = if (current >= 2) 0 else current + 1
            nbt.putInt(SPEED_TIER_KEY, next)
            return next
        }

        fun getSpeedTier(stack: ItemStack): Int =
            stack.orCreateNbt.getInt(SPEED_TIER_KEY)

        /**
         * 服务端按一个实际推进 tick 扣除能量。
         * @return 能量充足且扣除成功时为 true；能量不足会自动关闭神行。
         */
        fun consumeSpeedEnergyTick(stack: ItemStack): Boolean {
            val leggings = stack.item as? QuantumLeggings ?: return false
            val nbt = stack.orCreateNbt
            val tier = nbt.getInt(SPEED_TIER_KEY)
            if (tier <= 0) return false

            val energy = leggings.getEnergy(stack)
            if (energy <= 0L) {
                nbt.putInt(SPEED_TIER_KEY, 0)
                return false
            }

            val euPerTick = if (tier == 1) {
                Ic2Config.getQuantumLeggingsEuPerTick() / 2.0
            } else {
                Ic2Config.getQuantumLeggingsEuPerTick()
            }
            var remainder = nbt.getDouble(SPEED_REMAINDER_KEY) + euPerTick
            val toConsume = remainder.toLong()
            if (toConsume > 0L) {
                if (energy < toConsume) {
                    nbt.putInt(SPEED_TIER_KEY, 0)
                    return false
                }
                leggings.setEnergy(stack, energy - toConsume)
                remainder -= toConsume
            }
            nbt.putDouble(SPEED_REMAINDER_KEY, remainder)
            return true
        }

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val casing = MachineCasingBlock::class.item()
            val lapotron = LapotronCrystalItem::class.instance()
            val iridium = IridiumPlate::class.instance()
            val nano = NanoLeggings::class.instance()
            if (casing == Items.AIR || lapotron == Items.AIR || iridium == Items.AIR || nano == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = QuantumLeggings::class.id(),
                result = QuantumLeggings::class.instance(),
                pattern = listOf("MLM", "INI", "G G"),
                keys = mapOf<Char, Item>(
                    'M' to casing,
                    'L' to lapotron,
                    'I' to iridium,
                    'N' to nano,
                    'G' to Items.GLOWSTONE_DUST
                ),
                category = "equipment"
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val tier = stack.orCreateNbt.getInt(SPEED_TIER_KEY)
        val energy = getEnergy(stack)
        val remainingSeconds = if (energy > 0 && maxCapacity > 0) {
            // 关闭时按下次开启的 1 档耗电估算；只有 2 档按全耗电估算。
            val divisor = if (tier == 2) 1.0 else 2.0
            energy.toDouble() / maxCapacity * speedBoostDurationSeconds * divisor
        } else 0.0
        val timeText = if (remainingSeconds >= 60) {
            val minutes = (remainingSeconds / 60).toInt()
            val seconds = (remainingSeconds % 60).toInt()
            "${minutes}分${seconds}秒"
        } else {
            "${remainingSeconds.toInt()}秒"
        }

        val tierText = when (tier) {
            1 -> Text.translatable("tooltip.ic2_120.armor.speed_tier.1")
            2 -> Text.translatable("tooltip.ic2_120.armor.speed_tier.2")
            else -> Text.translatable("tooltip.ic2_120.armor.speed_tier.off")
        }
        tooltip.add(Text.translatable("tooltip.ic2_120.quantum_leggings.speed", tierText, timeText).formatted(Formatting.GRAY))
        val pct = "%.0f".format(getDamageReduction() * 100)
        tooltip.add(Text.translatable("tooltip.ic2_120.armor.damage_reduction", pct).formatted(Formatting.GRAY))
    }
}

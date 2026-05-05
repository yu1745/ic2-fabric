package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.editCustomData
import ic2_120.getCustomData
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
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World
import kotlin.math.abs

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
 * - 1档：+20% 移速，半耗电（约 69 EU/t）
 * - 2档：+40% 移速，全耗电（约 139 EU/t）
 * - 满电 1档可跑 60 分钟，2档可跑 30 分钟
 * - 静止时不耗电
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_leggings", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumLeggings : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.LEGGINGS, Item.Settings().maxCount(1)) {

    companion object {
        private const val SPEED_TIER_KEY = "SpeedTier"
        private const val SPEED_REMAINDER_KEY = "QuantumLeggingsSpeedRemainder"

        val speedBoostDurationSeconds: Int
            get() = Ic2Config.current.armor.quantumLeggings.speedBoostDurationSeconds

        val speedMultiplierTier1: Double
            get() = Ic2Config.current.armor.quantumLeggings.speedMultiplierTier1

        val speedMultiplierTier2: Double
            get() = Ic2Config.current.armor.quantumLeggings.speedMultiplierTier2

        /**
         * 循环速度档位：0 → 1 → 2 → 0
         * @return 切换后的档位
         */
        fun cycleSpeedTier(stack: ItemStack): Int {
            val current = stack.getCustomData()?.getInt(SPEED_TIER_KEY) ?: 0
            val next = if (current >= 2) 0 else current + 1
            stack.editCustomData { it.putInt(SPEED_TIER_KEY, next) }
            return next
        }

        fun getSpeedTier(stack: ItemStack): Int =
            stack.getCustomData()?.getInt(SPEED_TIER_KEY) ?: 0

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
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

    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.LEGS) !== stack) return

        val nbt = stack.getCustomData() ?: return
        val tier = nbt.getInt(SPEED_TIER_KEY)
        if (tier <= 0) return

        // 检测玩家是否在移动
        val isMoving = abs(player.velocity.x) > 0.001 ||
                       abs(player.velocity.z) > 0.001
        if (!isMoving) {
            // 不动时不消耗能量，但保持 buff
            applySpeedEffect(player, tier)
            return
        }

        // 消耗能量
        val energy = getEnergy(stack)
        if (energy <= 0) {
            nbt.putInt(SPEED_TIER_KEY, 0)
            player.removeStatusEffect(StatusEffects.SPEED)
            return
        }

        val euPerTick = if (tier == 1) {
            Ic2Config.getQuantumLeggingsEuPerTick() / 2.0
        } else {
            Ic2Config.getQuantumLeggingsEuPerTick()
        }

        var remainder = nbt.getDouble(SPEED_REMAINDER_KEY)
        remainder += euPerTick
        val toConsume = remainder.toLong()
        if (toConsume > 0) {
            if (energy < toConsume) {
                nbt.putInt(SPEED_TIER_KEY, 0)
                player.removeStatusEffect(StatusEffects.SPEED)
                return
            }
            setEnergy(stack, energy - toConsume)
        }
        nbt.putDouble(SPEED_REMAINDER_KEY, remainder - toConsume)

        applySpeedEffect(player, tier)
    }

    private fun applySpeedEffect(player: PlayerEntity, tier: Int) {
        val amplifier = tier - 1  // tier 1 = amp 0 (+20%), tier 2 = amp 1 (+40%)
        player.addStatusEffect(
            StatusEffectInstance(StatusEffects.SPEED, 100, amplifier, true, false, true)
        )
    }

    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        val tier = stack.getCustomData()?.getInt(SPEED_TIER_KEY) ?: 0
        val energy = getEnergy(stack)
        val remainingSeconds = if (energy > 0 && maxCapacity > 0) {
            val divisor = if (tier == 1) 2.0 else 1.0
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
            1 -> "1档"
            2 -> "2档"
            else -> "关"
        }
        tooltip.add(Text.literal("神行: $tierText | 剩余: $timeText").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("减伤: 30%").formatted(Formatting.GRAY))
    }
}

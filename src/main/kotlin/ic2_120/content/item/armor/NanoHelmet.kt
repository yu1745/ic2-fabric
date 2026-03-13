package ic2_120.content.item.armor

import ic2_120.Ic2_120
import ic2_120.content.item.ModArmorMaterials
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World

/**
 * 纳米头盔 (Nano Helmet)
 *
 * 纳米套装的头盔部件，提供夜视功能。
 *
 * ## 核心参数
 *
 * - 载电量：1,000,000 EU（1 MEU）
 * - 能量等级：3
 * - 减伤比例：15%
 *
 * ## 夜视功能
 *
 * - 耗电：278 EU/tick（1M EU / 1小时 = 72000 ticks）
 * - 快捷键：Alt + N
 * - 光线 >= 8 时移除夜视并添加致盲
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有纳米装备均匀扣除
 */
@ModItem(name = "nano_helmet", tab = CreativeTab.IC2_MATERIALS, group = "nano_armor")
class NanoHelmet : NanoArmorItem(ModArmorMaterials.NANO_ARMOR, ArmorItem.Type.HELMET, FabricItemSettings().maxCount(1)) {

    companion object {
        private const val NIGHT_VISION_COST = 14L  // 1M EU / 1h = 72000 ticks ≈ 14 EU/t
        private const val NIGHT_VISION_DURATION = 220
        private const val BLINDNESS_DURATION = 80
        private const val NIGHT_VISION_KEY = "NightVisionEnabled"

        fun toggleNightVision(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBoolean(NIGHT_VISION_KEY)
            nbt.putBoolean(NIGHT_VISION_KEY, enabled)
            return enabled
        }
    }

    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.HEAD) !== stack) return

        val nbt = stack.orCreateNbt
        if (!nbt.getBoolean(NIGHT_VISION_KEY)) return

        val energy = getEnergy(stack)
        if (energy < NIGHT_VISION_COST) {
            setEnergy(stack, 0)
            nbt.putBoolean(NIGHT_VISION_KEY, false)
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            return
        }

        // 消耗能量
        setEnergy(stack, energy - NIGHT_VISION_COST)

        // 光线检测（复用 NightVisionGoggles 逻辑）
        val brightness = world.getLightLevel(player.blockPos)
        if (brightness >= 8) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            val blind = player.getStatusEffect(StatusEffects.BLINDNESS)
            if (blind == null || blind.duration <= 0) {
                player.addStatusEffect(
                    StatusEffectInstance(StatusEffects.BLINDNESS, BLINDNESS_DURATION, 0, true, false, true)
                )
            }
        } else {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, true, false, true)
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val nvEnabled = stack.orCreateNbt.getBoolean(NIGHT_VISION_KEY)
        val energy = getEnergy(stack)

        // 计算夜视剩余时间（分钟）
        val remainingMinutes = if (energy > 0 && nvEnabled) {
            val ticks = energy / NIGHT_VISION_COST
            val seconds = ticks / 20.0
            val minutes = seconds / 60.0
            "%.1f".format(minutes)
        } else "N/A"

        tooltip.add(Text.literal("夜视: ${if (nvEnabled) "§aON" else "§cOFF"} §8[${remainingMinutes}分钟]").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("减伤: 11.54%").formatted(Formatting.GRAY))
    }
}

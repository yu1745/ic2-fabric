package ic2_120.content.item.armor

import ic2_120.compat.orCreateNbt

import ic2_120.compat.isClient

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.level.Level

/**
 * 纳米头盔 (Nano Helmet)
 *
 * 纳米套装的头盔部件，提供夜视功能。
 *
 * ## 核心参数
 *
 * - 载电量：1,000,000 EU（1 MEU）
 * - 能量等级：3
 * - 减伤比例：12%   （量子 15% × 0.8）
 *
 * ## 夜视功能
 *
 * - 耗电：278 EU/tick（1M EU / 1小时 = 72000 ticks）
 * - 快捷键：Alt + N
 * - 光线 >= 8 时移除夜视并添加致盲
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 20000 EU × 配置倍率（默认倍率 1.0）
 * - 能量从所有纳米装备均匀扣除
 */
@ModItem(name = "nano_helmet", tab = CreativeTab.IC2_MATERIALS, group = "nano_armor")
class NanoHelmet : NanoArmorItem(ModArmorMaterials.NANO_ARMOR, ArmorItem.Type.HELMET, Item.Properties().stacksTo(1)) {

    companion object {
        private const val NIGHT_VISION_DURATION = 220
        private const val BLINDNESS_DURATION = 80
        private const val NIGHT_VISION_KEY = "NightVisionEnabled"
        private const val NV_REMAINDER_KEY = "NanoHelmetNvRemainder"

        val nightVisionCostPerTick: Double
            get() = Ic2Config.getNanoHelmetNightVisionEuPerTick()

        val nightVisionDurationSeconds: Int
            get() = Ic2Config.current.armor.nightVision.nanoHelmetDurationSeconds

        fun toggleNightVision(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBoolean(NIGHT_VISION_KEY)
            nbt.putBoolean(NIGHT_VISION_KEY, enabled)
            return enabled
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = CarbonPlate::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            val goggles = NightVisionGoggles::class.instance()
            if (plate == Items.AIR || crystal == Items.AIR || goggles == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = NanoHelmet::class.id(),
                result = NanoHelmet::class.instance(),
                pattern = listOf("CEC", "CNC", "   "),
                keys = mapOf<Char, Item>(
                    'C' to plate,
                    'E' to crystal,
                    'N' to goggles
                ),
                category = "equipment"
            )
        }
    }

    override fun inventoryTick(stack: ItemStack, world: Level, entity: net.minecraft.world.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? Player ?: return
        if (player.getItemBySlot(EquipmentSlot.HEAD) !== stack) return

        val nbt = stack.orCreateNbt
        if (!nbt.getBoolean(NIGHT_VISION_KEY)) return

        val energy = getEnergy(stack)
        if (energy <= 0) {
            setEnergy(stack, 0)
            nbt.putBoolean(NIGHT_VISION_KEY, false)
            player.removeEffect(MobEffects.NIGHT_VISION)
            return
        }

        // 使用余数累加器消耗能量
        var remainder = nbt.getDouble(NV_REMAINDER_KEY)
        remainder += nightVisionCostPerTick
        val toConsume = remainder.toLong()
        if (toConsume > 0) {
            if (energy < toConsume) {
                setEnergy(stack, 0)
                nbt.putBoolean(NIGHT_VISION_KEY, false)
                player.removeEffect(MobEffects.NIGHT_VISION)
                return
            }
            setEnergy(stack, energy - toConsume)
        }
        nbt.putDouble(NV_REMAINDER_KEY, remainder - toConsume)

        // 光线检测（复用 NightVisionGoggles 逻辑）
        val brightness = world.getMaxLocalRawBrightness(player.blockPosition())
        if (brightness >= 8) {
            player.removeEffect(MobEffects.NIGHT_VISION)
            val blind = player.getEffect(MobEffects.BLINDNESS)
            if (blind == null || blind.duration <= 0) {
                player.addEffect(
                    MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, 0, true, false, true)
                )
            }
        } else {
            player.addEffect(
                MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, true, false, true)
            )
        }
    }

    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, type)
        val nvEnabled = stack.orCreateNbt.getBoolean(NIGHT_VISION_KEY)
        val energy = getEnergy(stack)

        val remainingSeconds = if (energy > 0 && maxCapacity > 0) {
            energy.toDouble() / maxCapacity * nightVisionDurationSeconds
        } else 0.0
        val timeText = if (remainingSeconds >= 60) {
            val minutes = (remainingSeconds / 60).toInt()
            val seconds = (remainingSeconds % 60).toInt()
            "${minutes}分${seconds}秒"
        } else {
            "${remainingSeconds.toInt()}秒"
        }

                tooltip.add(Component.translatable("tooltip.ic2_120.nano_helmet.night_vision",
            Component.translatable(if (nvEnabled) "tooltip.ic2_120.status.on" else "tooltip.ic2_120.status.off"),
            timeText
        ).withStyle(ChatFormatting.GRAY))
        val pct = "%.2f".format(getDamageReduction() * 100).trimEnd('0').trimEnd('.')
        tooltip.add(Component.translatable("tooltip.ic2_120.armor.damage_reduction", pct).withStyle(ChatFormatting.GRAY))
    }
}

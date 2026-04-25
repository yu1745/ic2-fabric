package ic2_120.content.item.armor

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
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import ic2_120.getOrCreateCustomData

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
class NanoHelmet : NanoArmorItem(ModArmorMaterials.NANO_ARMOR, ArmorItem.Type.HELMET, Item.Settings().maxCount(1)) {

    companion object {
        private const val NIGHT_VISION_DURATION = 220
        private const val BLINDNESS_DURATION = 80
        private const val NIGHT_VISION_KEY = "NightVisionEnabled"

        val nightVisionCostPerTick: Long
            get() = Ic2Config.getNanoHelmetNightVisionEuPerTick()

        fun toggleNightVision(stack: ItemStack): Boolean {
            val nbt = stack.getOrCreateCustomData()
            val enabled = !nbt.getBoolean(NIGHT_VISION_KEY)
            nbt.putBoolean(NIGHT_VISION_KEY, enabled)
            return enabled
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
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

    override fun inventoryTick(stack: ItemStack, world: net.minecraft.world.World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.HEAD) !== stack) return

        val nbt = stack.getOrCreateCustomData()
        if (!nbt.getBoolean(NIGHT_VISION_KEY)) return

        val energy = getEnergy(stack)
        if (energy < nightVisionCostPerTick) {
            setEnergy(stack, 0)
            nbt.putBoolean(NIGHT_VISION_KEY, false)
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            return
        }

        // 消耗能量
        setEnergy(stack, energy - nightVisionCostPerTick)

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

    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        val nvEnabled = stack.getOrCreateCustomData().getBoolean(NIGHT_VISION_KEY)
        val energy = getEnergy(stack)

        // 计算夜视剩余时间（分钟）
        val remainingMinutes = if (energy > 0 && nvEnabled) {
            val ticks = energy / nightVisionCostPerTick
            val seconds = ticks / 20.0
            val minutes = seconds / 60.0
            "%.1f".format(minutes)
        } else "N/A"

        tooltip.add(Text.literal("夜视: ${if (nvEnabled) "§aON" else "§cOFF"} §8[${remainingMinutes}分钟]").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("减伤: 11.54%").formatted(Formatting.GRAY))
    }
}

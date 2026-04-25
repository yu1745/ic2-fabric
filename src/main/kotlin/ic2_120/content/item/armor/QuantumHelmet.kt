package ic2_120.content.item.armor

import ic2_120.Ic2_120
import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.content.effect.ModStatusEffects
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.HazmatHelmet
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
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.world.World
import ic2_120.editCustomData
import ic2_120.getCustomData

/**
 * 量子头盔 (Quantum Helmet)
 *
 * 量子套装的头盔部件，提供多种辅助功能。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：15%
 *
 * ## 特殊功能
 *
 * 1. **夜视**：694 EU/tick（10M EU / 8小时 = 576000 ticks）
 * 2. **水下呼吸**：消耗 air_cell → empty_cell
 * 3. **补充饱食度**：饱食度不满自动从背包拿满锡罐食用，1000 EU/次
 * 4. **消除 debuff**：需全套量子护甲，中毒/凋零/辐射，100 EU/次
 *
 * **夜视快捷键**：Alt + N
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_helmet", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumHelmet : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.HELMET, Item.Settings().maxCount(1)) {

    companion object {
        private const val NIGHT_VISION_COST = 17L  // 10M EU / 8h = 576000 ticks ≈ 17 EU/t
        private const val FOOD_FILL_COST = 1000L
        private const val CURE_EFFECT_COST = 100L
        private const val NIGHT_VISION_KEY = "NightVisionEnabled"
        private const val AIR_CHECK_COOLDOWN_KEY = "AirCheckCooldown"
        private const val FOOD_CHECK_COOLDOWN_KEY = "FoodCheckCooldown"
        private const val AIR_THRESHOLD = 60

        fun toggleNightVision(stack: ItemStack): Boolean {
            val enabled = !(stack.getCustomData()?.getBoolean(NIGHT_VISION_KEY) ?: false)
            stack.editCustomData { it.putBoolean(NIGHT_VISION_KEY, enabled) }
            return enabled
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val glass = ReinforcedGlassBlock::class.item()
            val nano = NanoHelmet::class.instance()
            val adv = AdvancedCircuit::class.instance()
            val lapotron = LapotronCrystalItem::class.instance()
            val iridium = IridiumPlate::class.instance()
            val hazmat = HazmatHelmet::class.instance()
            if (glass == Items.AIR || nano == Items.AIR || adv == Items.AIR || lapotron == Items.AIR || iridium == Items.AIR || hazmat == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = QuantumHelmet::class.id(),
                result = QuantumHelmet::class.instance(),
                pattern = listOf("GNG", "RLR", "AHA"),
                keys = mapOf<Char, Item>(
                    'G' to glass,
                    'N' to nano,
                    'R' to adv,
                    'L' to lapotron,
                    'A' to iridium,
                    'H' to hazmat
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

        val nbt = stack.getCustomData()
        var energy = getEnergy(stack)

        // 功能 1: 水下呼吸（复用 HazmatHelmet 逻辑）
        if (player.isTouchingWater && player.air <= AIR_THRESHOLD) {
            val cooldown = nbt?.getInt(AIR_CHECK_COOLDOWN_KEY) ?: 0
            if (cooldown <= 0) {
                if (consumeAirCellIfAvailable(player)) {
                    player.air = player.maxAir
                    stack.editCustomData { it.putInt(AIR_CHECK_COOLDOWN_KEY, 20) }  // 1 秒冷却
                }
            } else {
                stack.editCustomData { it.putInt(AIR_CHECK_COOLDOWN_KEY, cooldown - 1) }
            }
        }

        // 功能 2: 夜视
        if (nbt?.getBoolean(NIGHT_VISION_KEY) == true) {
            if (energy >= NIGHT_VISION_COST) {
                energy -= NIGHT_VISION_COST
                applyNightVisionEffect(player, world)
            } else {
                stack.editCustomData { it.putBoolean(NIGHT_VISION_KEY, false) }
                player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            }
        }

        // 功能 3: 补充饱食度（自动从背包拿满锡罐食用）
        if (player.hungerManager.foodLevel < 20 &&
            energy >= FOOD_FILL_COST
        ) {
            val cooldown = nbt?.getInt(FOOD_CHECK_COOLDOWN_KEY) ?: 0
            if (cooldown <= 0) {
                if (consumeFilledTinCanIfAvailable(player)) {
                    energy -= FOOD_FILL_COST
                    player.hungerManager.foodLevel = 20
                    player.hungerManager.saturationLevel = 5f
                    stack.editCustomData { it.putInt(FOOD_CHECK_COOLDOWN_KEY, 20) }  // 1 秒冷却
                }
            } else {
                stack.editCustomData { it.putInt(FOOD_CHECK_COOLDOWN_KEY, cooldown - 1) }
            }
        }

        // 功能 4: 消除负面效果（需全套量子护甲）
        if (QuantumArmorItem.hasFullQuantumArmor(player) && energy >= CURE_EFFECT_COST) {
            val harmfulEffects = listOf(StatusEffects.POISON, StatusEffects.WITHER, ModStatusEffects.RADIATION)
            for (effect in harmfulEffects) {
                if (player.hasStatusEffect(effect)) {
                    player.removeStatusEffect(effect)
                    energy -= CURE_EFFECT_COST
                    break
                }
            }
        }

        setEnergy(stack, energy)
    }

    private fun consumeAirCellIfAvailable(player: PlayerEntity): Boolean {
        val airCellItem = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "air_cell"))
        val emptyCellItem = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell"))
        val slot = player.inventory.main.find { !it.isEmpty && it.item === airCellItem }
        if (slot != null) {
            slot.decrement(1)
            val emptyCell = ItemStack(emptyCellItem)
            if (!player.inventory.insertStack(emptyCell)) {
                player.dropItem(emptyCell, false)
            }
            return true
        }
        return false
    }

    private fun consumeFilledTinCanIfAvailable(player: PlayerEntity): Boolean {
        val filledCanItem = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "filled_tin_can"))
        val emptyCanItem = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "tin_can"))
        val slot = player.inventory.main.find { !it.isEmpty && it.item === filledCanItem }
        if (slot != null) {
            slot.decrement(1)
            val emptyCan = ItemStack(emptyCanItem)
            if (!player.inventory.insertStack(emptyCan)) {
                player.dropItem(emptyCan, false)
            }
            return true
        }
        return false
    }

    private fun applyNightVisionEffect(player: PlayerEntity, world: net.minecraft.world.World) {
        val brightness = world.getLightLevel(player.blockPos)
        if (brightness >= 8) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
        } else {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.NIGHT_VISION, 220, 0, true, false, true)
            )
        }
    }

    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        val nvEnabled = stack.getCustomData()?.getBoolean(NIGHT_VISION_KEY) ?: false
        val energy = getEnergy(stack)

        // 计算夜视剩余时间（分钟）
        val remainingMinutes = if (energy > 0 && nvEnabled) {
            val ticks = energy / NIGHT_VISION_COST
            val seconds = ticks / 20.0
            val minutes = seconds / 60.0
            "%.1f".format(minutes)
        } else "N/A"

        tooltip.add(Text.literal("夜视: ${if (nvEnabled) "§aON" else "§cOFF"} §8[${remainingMinutes}分钟]").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("减伤: 15% | 水下呼吸 | 夜视 | 饱食不满自动吃满锡罐").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("全套时: 消除debuff").formatted(Formatting.GRAY))
    }
}

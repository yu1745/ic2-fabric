package ic2_120.content.item.armor

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.compat.orCreateNbt

import ic2_120.compat.isClient

import ic2_120.config.Ic2Config
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
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.data.recipes.RecipeOutput
import ic2_120.compat.ArmorItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import java.util.function.Consumer

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
 * - 每减免 1 点伤害消耗 20000 EU × 配置倍率（默认倍率 1.0）
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_helmet", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumHelmet : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.HELMET, Item.Properties().stacksTo(1)) {

    companion object {
        private const val FOOD_FILL_COST = 1000L
        private const val CURE_EFFECT_COST = 100L
        private const val NIGHT_VISION_KEY = "NightVisionEnabled"
        private const val AIR_CHECK_COOLDOWN_KEY = "AirCheckCooldown"
        private const val FOOD_CHECK_COOLDOWN_KEY = "FoodCheckCooldown"
        private const val AIR_THRESHOLD = 60
        private const val NV_REMAINDER_KEY = "QuantumHelmetNvRemainder"

        val nightVisionCostPerTick: Double
            get() = Ic2Config.getQuantumHelmetNightVisionEuPerTick()

        val nightVisionDurationSeconds: Int
            get() = Ic2Config.current.armor.quantumHelmet.nightVisionDurationSeconds

        fun toggleNightVision(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBooleanOr(NIGHT_VISION_KEY, false)
            nbt.putBoolean(NIGHT_VISION_KEY, enabled)
            return enabled
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
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

    override fun inventoryTick(stack: ItemStack, world: net.minecraft.server.level.ServerLevel, entity: net.minecraft.world.entity.Entity, slot: net.minecraft.world.entity.EquipmentSlot?) {
        super.inventoryTick(stack, world, entity, slot)
        if (world.isClient) return

        val player = entity as? Player ?: return
        if (player.getItemBySlot(EquipmentSlot.HEAD) !== stack) return

        val nbt = stack.orCreateNbt
        var energy = getEnergy(stack)

        // 功能 1: 水下呼吸（复用 HazmatHelmet 逻辑）
        if (player.isInWater && player.airSupply <= AIR_THRESHOLD) {
            val cooldown = nbt.getIntOr(AIR_CHECK_COOLDOWN_KEY, 0)
            if (cooldown <= 0) {
                if (consumeAirCellIfAvailable(player, world)) {
                    player.airSupply = player.maxAirSupply
                    nbt.putInt(AIR_CHECK_COOLDOWN_KEY, 20)  // 1 秒冷却
                }
            } else {
                nbt.putInt(AIR_CHECK_COOLDOWN_KEY, cooldown - 1)
            }
        }

        // 功能 2: 夜视（使用余数累加器精确消耗）
        if (nbt.getBooleanOr(NIGHT_VISION_KEY, false)) {
            if (energy > 0) {
                var remainder = nbt.getDoubleOr(NV_REMAINDER_KEY, 0.0)
                remainder += nightVisionCostPerTick
                val toConsume = remainder.toLong()
                if (toConsume > 0) {
                    if (energy < toConsume) {
                        nbt.putBoolean(NIGHT_VISION_KEY, false)
                        player.removeEffect(MobEffects.NIGHT_VISION)
                        energy = 0
                    } else {
                        energy -= toConsume
                        applyNightVisionEffect(player, world)
                    }
                } else {
                    applyNightVisionEffect(player, world)
                }
                nbt.putDouble(NV_REMAINDER_KEY, remainder - (if (toConsume > 0 && energy > 0) toConsume else 0))
            } else {
                nbt.putBoolean(NIGHT_VISION_KEY, false)
                player.removeEffect(MobEffects.NIGHT_VISION)
            }
        }

        // 功能 3: 补充饱食度（自动从背包拿满锡罐食用）
        if (player.foodData.foodLevel < 20 &&
            energy >= FOOD_FILL_COST
        ) {
            val cooldown = nbt.getIntOr(FOOD_CHECK_COOLDOWN_KEY, 0)
            if (cooldown <= 0) {
                if (consumeFilledTinCanIfAvailable(player, world)) {
                    energy -= FOOD_FILL_COST
                    player.foodData.setFoodLevel(20)
                    player.foodData.setSaturation(5f)
                    nbt.putInt(FOOD_CHECK_COOLDOWN_KEY, 20)  // 1 秒冷却
                }
            } else {
                nbt.putInt(FOOD_CHECK_COOLDOWN_KEY, cooldown - 1)
            }
        }

        // 功能 4: 消除负面效果（需全套量子护甲）
        if (QuantumArmorItem.hasFullQuantumArmor(player) && energy >= CURE_EFFECT_COST) {
            // 按效果类别动态覆盖所有负面效果（包括坚守者的 DARKNESS 以及模组效果）。
            val harmfulEffects = player.activeEffects
                .filter {
                    it.effect.value().category == MobEffectCategory.HARMFUL ||
                        it.effect.value() === ModStatusEffects.RADIATION
                }
            for (effect in harmfulEffects) {
                if (energy < CURE_EFFECT_COST) break
                player.removeEffect(effect.effect)
                energy -= CURE_EFFECT_COST
            }
        }

        setEnergy(stack, energy)
    }

    private fun consumeAirCellIfAvailable(player: Player, world: net.minecraft.server.level.ServerLevel): Boolean {
        val airCellItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "air_cell"))
        val emptyCellItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "empty_cell"))
        val slot = player.inventory.nonEquipmentItems.find { !it.isEmpty && it.item === airCellItem }
        if (slot != null) {
            slot.shrink(1)
            val emptyCell = ItemStack(emptyCellItem)
            if (!player.inventory.add(emptyCell)) {
                player.spawnAtLocation(world, emptyCell)
            }
            return true
        }
        return false
    }

    private fun consumeFilledTinCanIfAvailable(player: Player, world: net.minecraft.server.level.ServerLevel): Boolean {
        val filledCanItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "filled_tin_can"))
        val emptyCanItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "tin_can"))
        val slot = player.inventory.nonEquipmentItems.find { !it.isEmpty && it.item === filledCanItem }
        if (slot != null) {
            slot.shrink(1)
            val emptyCan = ItemStack(emptyCanItem)
            if (!player.inventory.add(emptyCan)) {
                player.spawnAtLocation(world, emptyCan)
            }
            return true
        }
        return false
    }

    private fun applyNightVisionEffect(player: Player, world: Level) {
        val brightness = world.getMaxLocalRawBrightness(player.blockPosition())
        if (brightness >= 8) {
            player.removeEffect(MobEffects.NIGHT_VISION)
        } else {
            player.addEffect(
                MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, true)
            )
        }
    }

    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, display: net.minecraft.world.item.component.TooltipDisplay, consumer: java.util.function.Consumer<Component>, type: TooltipFlag) {
        val tooltip = ic2_120.compat.TooltipSinkList(consumer)
        super.appendHoverText(stack, context, display, consumer, type)
        val nvEnabled = stack.orCreateNbt.getBooleanOr(NIGHT_VISION_KEY, false)
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

        tooltip.add(Component.translatable("tooltip.ic2_120.quantum_helmet.night_vision",
            Component.translatable(if (nvEnabled) "tooltip.ic2_120.status.on" else "tooltip.ic2_120.status.off"),
            timeText
        ).withStyle(ChatFormatting.GRAY))
        val pct = "%.0f".format(getDamageReduction() * 100)
        tooltip.add(Component.translatable("tooltip.ic2_120.quantum_helmet.features", pct).withStyle(ChatFormatting.GRAY))
        tooltip.add(Component.translatable("tooltip.ic2_120.quantum_helmet.full_set").withStyle(ChatFormatting.GRAY))
    }
}

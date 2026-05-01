package ic2_120.content.item.energy

import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.block.cables.InsulatedTinCableBlock
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.AdvancedHeatExchangerItem
import ic2_120.content.item.BronzeCasing
import ic2_120.content.item.CoalFuelDust
import ic2_120.content.item.Circuit
import ic2_120.content.item.ComponentHeatExchangerItem
import ic2_120.content.item.HeatExchangerItem
import ic2_120.content.item.LapisDust
import ic2_120.content.item.LeadDust
import ic2_120.content.item.SulfurDust
import ic2_120.content.item.TinCasing
import ic2_120.content.item.TinIngot
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory

// ========== 普通充电电池 ==========

/**
 * 充电电池（Re-Battery）
 *
 * 最基础的电池，可作为便携式电源使用。
 *
 * @spec 等级1, 容量10,000 EU, 速度32 EU/t
 */
@ModItem(name = "re_battery", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class ReBatteryItem : BatteryItemBase(
    name = "re_battery", tier = 1, maxCapacity = 10_000, canChargeWireless = false
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val tinCable = InsulatedTinCableBlock::class.item()
            val copperCable = InsulatedCopperCableBlock::class.item()
            val tinCasing = TinCasing::class.instance()
            val tinIngot = TinIngot::class.instance()
            val redstone = Items.REDSTONE

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReBatteryItem::class.instance(), 1)
                .pattern(" W ")
                .pattern("CRC")
                .pattern("CRC")
                .input('W', tinCable)
                .input('C', tinCasing)
                .input('R', redstone)
                .criterion(hasItem(tinCable), conditionsFromItem(tinCable))
                .offerTo(exporter, ReBatteryItem::class.recipeId("from_tin_casing"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReBatteryItem::class.instance(), 1)
                .pattern(" W ")
                .pattern("IRI")
                .pattern("IRI")
                .input('W', copperCable)
                .input('I', tinIngot)
                .input('R', redstone)
                .criterion(hasItem(copperCable), conditionsFromItem(copperCable))
                .offerTo(exporter, ReBatteryItem::class.recipeId("from_tin_ingots"))
        }
    }
}

/**
 * 高级充电电池（Advanced Re-Battery）
 *
 * @spec 等级2, 容量100,000 EU, 速度128 EU/t
 */
@ModItem(name = "advanced_re_battery", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class AdvancedReBatteryItem : BatteryItemBase(
    name = "advanced_re_battery", tier = 2, maxCapacity = 100_000, canChargeWireless = false
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val copperCable = InsulatedCopperCableBlock::class.item()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedReBatteryItem::class.instance(), 1)
                .pattern("WBW")
                .pattern("BSB")
                .pattern("BLB")
                .input('W', copperCable)
                .input('B', BronzeCasing::class.instance())
                .input('S', SulfurDust::class.instance())
                .input('L', LeadDust::class.instance())
                .criterion(hasItem(copperCable), conditionsFromItem(copperCable))
                .offerTo(exporter, AdvancedReBatteryItem::class.recipeId())
        }
    }
}

/**
 * 能量水晶（Energy Crystal）
 *
 * @spec 等级3, 容量1,000,000 EU, 速度512 EU/t
 *
 * 右键使用可给背包中其他带电物品充电
 */
@ModItem(name = "energy_crystal", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class EnergyCrystalItem : BatteryItemBase(
    name = "energy_crystal", tier = 3, maxCapacity = 1_000_000, canChargeWireless = false
) {
    override fun use(world: net.minecraft.world.World, user: net.minecraft.entity.player.PlayerEntity, hand: net.minecraft.util.Hand): net.minecraft.util.TypedActionResult<net.minecraft.item.ItemStack> {
        val crystal = user.getStackInHand(hand)
        if (world.isClient) return net.minecraft.util.TypedActionResult.success(crystal)

        // 获取水晶当前电量
        val availableEnergy = getCurrentCharge(crystal)
        if (availableEnergy <= 0) {
            user.sendMessage(net.minecraft.text.Text.literal("§c能量水晶电量不足！"), true)
            return net.minecraft.util.TypedActionResult.fail(crystal)
        }

        var totalCharged = 0L
        var itemsCharged = 0
        var energyRemaining = availableEnergy

        // 遍历玩家主背包
        for (slot in 0..<user.inventory.main.size) {
            if (energyRemaining <= 0) break

            // 跳过当前手持的水晶
            val stack = user.inventory.main[slot]
            if (stack.isEmpty || stack === crystal) continue

            // 检查物品是否可以被充电
            if (!stack.canBeCharged()) continue

            // 获取目标物品的电池接口
            val targetBattery = stack.item as? IBatteryItem
            val targetTool = stack.item as? IElectricTool

            // 计算可以充入的电量
            val targetCurrent = targetBattery?.getCurrentCharge(stack) ?: targetTool?.getEnergy(stack) ?: 0
            val targetMax = targetBattery?.maxCapacity ?: targetTool?.getMaxEnergy() ?: 0
            val canAccept = targetMax - targetCurrent

            if (canAccept > 0) {
                // 计算实际充入电量（受水晶剩余电量和目标容量限制）
                val toCharge = minOf(energyRemaining, canAccept)

                // 执行充电
                targetBattery?.charge(stack, toCharge)
                    ?: targetTool?.setEnergy(stack, targetCurrent + toCharge)

                energyRemaining -= toCharge
                totalCharged += toCharge
                itemsCharged++
            }
        }

        // 遍历玩家护甲栏
        for (slot in 0..<user.inventory.armor.size) {
            if (energyRemaining <= 0) break

            val stack = user.inventory.armor[slot]
            if (stack.isEmpty || !stack.canBeCharged()) continue

            val targetBattery = stack.item as? IBatteryItem
            val targetTool = stack.item as? IElectricTool

            val targetCurrent = targetBattery?.getCurrentCharge(stack) ?: targetTool?.getEnergy(stack) ?: 0
            val targetMax = targetBattery?.maxCapacity ?: targetTool?.getMaxEnergy() ?: 0
            val canAccept = targetMax - targetCurrent

            if (canAccept > 0) {
                val toCharge = minOf(energyRemaining, canAccept)
                targetBattery?.charge(stack, toCharge)
                    ?: targetTool?.setEnergy(stack, targetCurrent + toCharge)
                energyRemaining -= toCharge
                totalCharged += toCharge
                itemsCharged++
            }
        }

        // 遍历副手
        val offhandStack = user.offHandStack
        if (energyRemaining > 0 && !offhandStack.isEmpty && offhandStack.canBeCharged() && offhandStack !== crystal) {
            val targetBattery = offhandStack.item as? IBatteryItem
            val targetTool = offhandStack.item as? IElectricTool

            val targetCurrent = targetBattery?.getCurrentCharge(offhandStack) ?: targetTool?.getEnergy(offhandStack) ?: 0
            val targetMax = targetBattery?.maxCapacity ?: targetTool?.getMaxEnergy() ?: 0
            val canAccept = targetMax - targetCurrent

            if (canAccept > 0) {
                val toCharge = minOf(energyRemaining, canAccept)
                targetBattery?.charge(offhandStack, toCharge)
                    ?: targetTool?.setEnergy(offhandStack, targetCurrent + toCharge)
                energyRemaining -= toCharge
                totalCharged += toCharge
                itemsCharged++
            }
        }

        // 更新水晶电量
        val usedEnergy = availableEnergy - energyRemaining
        if (usedEnergy > 0) {
            setCurrentCharge(crystal, energyRemaining)

            // 发送反馈消息
            val formattedEnergy = "%,d".format(usedEnergy)
            val percentUsed = (usedEnergy * 100 / maxCapacity).toInt()
            user.sendMessage(
                net.minecraft.text.Text.literal("§a已充能 $itemsCharged 件物品，消耗 §e$formattedEnergy EU§a (${percentUsed}%)"),
                true
            )
        } else {
            user.sendMessage(net.minecraft.text.Text.literal("§c没有物品需要充电！"), true)
        }

        return net.minecraft.util.TypedActionResult.success(crystal)
    }
}

/**
 * 蓝波顿水晶（Lapotron Crystal）
 *
 * @spec 等级4, 容量10,000,000 EU, 速度2,048 EU/t
 *
 * 右键使用可给背包中其他带电物品充电
 */
@ModItem(name = "lapotron_crystal", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class LapotronCrystalItem : BatteryItemBase(
    name = "lapotron_crystal", tier = 4, maxCapacity = 10_000_000, canChargeWireless = false
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 含能量水晶：使用 battery_energy_shaped 继承输入水晶电量
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LapotronCrystalItem::class.recipeId(),
                result = LapotronCrystalItem::class.instance(),
                pattern = listOf("LCL", "LEL", "LCL"),
                keys = mapOf(
                    'L' to LapisDust::class.instance(),
                    'C' to AdvancedCircuit::class.instance(),
                    'E' to EnergyCrystalItem::class.instance()
                )
            )
        }
    }

    override fun use(world: net.minecraft.world.World, user: net.minecraft.entity.player.PlayerEntity, hand: net.minecraft.util.Hand): net.minecraft.util.TypedActionResult<net.minecraft.item.ItemStack> {
        val crystal = user.getStackInHand(hand)
        if (world.isClient) return net.minecraft.util.TypedActionResult.success(crystal)

        // 获取水晶当前电量
        val availableEnergy = getCurrentCharge(crystal)
        if (availableEnergy <= 0) {
            user.sendMessage(net.minecraft.text.Text.literal("§c蓝波顿水晶电量不足！"), true)
            return net.minecraft.util.TypedActionResult.fail(crystal)
        }

        var totalCharged = 0L
        var itemsCharged = 0
        var energyRemaining = availableEnergy

        // 遍历玩家主背包
        for (slot in 0..<user.inventory.main.size) {
            if (energyRemaining <= 0) break

            // 跳过当前手持的水晶
            val stack = user.inventory.main[slot]
            if (stack.isEmpty || stack === crystal) continue

            // 检查物品是否可以被充电
            if (!stack.canBeCharged()) continue

            // 获取目标物品的电池接口
            val targetBattery = stack.item as? IBatteryItem
            val targetTool = stack.item as? IElectricTool

            // 计算可以充入的电量
            val targetCurrent = targetBattery?.getCurrentCharge(stack) ?: targetTool?.getEnergy(stack) ?: 0
            val targetMax = targetBattery?.maxCapacity ?: targetTool?.getMaxEnergy() ?: 0
            val canAccept = targetMax - targetCurrent

            if (canAccept > 0) {
                // 计算实际充入电量（受水晶剩余电量和目标容量限制）
                val toCharge = minOf(energyRemaining, canAccept)

                // 执行充电
                targetBattery?.charge(stack, toCharge)
                    ?: targetTool?.setEnergy(stack, targetCurrent + toCharge)

                energyRemaining -= toCharge
                totalCharged += toCharge
                itemsCharged++
            }
        }

        // 遍历玩家护甲栏
        for (slot in 0..<user.inventory.armor.size) {
            if (energyRemaining <= 0) break

            val stack = user.inventory.armor[slot]
            if (stack.isEmpty || !stack.canBeCharged()) continue

            val targetBattery = stack.item as? IBatteryItem
            val targetTool = stack.item as? IElectricTool

            val targetCurrent = targetBattery?.getCurrentCharge(stack) ?: targetTool?.getEnergy(stack) ?: 0
            val targetMax = targetBattery?.maxCapacity ?: targetTool?.getMaxEnergy() ?: 0
            val canAccept = targetMax - targetCurrent

            if (canAccept > 0) {
                val toCharge = minOf(energyRemaining, canAccept)
                targetBattery?.charge(stack, toCharge)
                    ?: targetTool?.setEnergy(stack, targetCurrent + toCharge)
                energyRemaining -= toCharge
                totalCharged += toCharge
                itemsCharged++
            }
        }

        // 遍历副手
        val offhandStack = user.offHandStack
        if (energyRemaining > 0 && !offhandStack.isEmpty && offhandStack.canBeCharged() && offhandStack !== crystal) {
            val targetBattery = offhandStack.item as? IBatteryItem
            val targetTool = offhandStack.item as? IElectricTool

            val targetCurrent = targetBattery?.getCurrentCharge(offhandStack) ?: targetTool?.getEnergy(offhandStack) ?: 0
            val targetMax = targetBattery?.maxCapacity ?: targetTool?.getMaxEnergy() ?: 0
            val canAccept = targetMax - targetCurrent

            if (canAccept > 0) {
                val toCharge = minOf(energyRemaining, canAccept)
                targetBattery?.charge(offhandStack, toCharge)
                    ?: targetTool?.setEnergy(offhandStack, targetCurrent + toCharge)
                energyRemaining -= toCharge
                totalCharged += toCharge
                itemsCharged++
            }
        }

        // 更新水晶电量
        val usedEnergy = availableEnergy - energyRemaining
        if (usedEnergy > 0) {
            setCurrentCharge(crystal, energyRemaining)

            // 发送反馈消息
            val formattedEnergy = "%,d".format(usedEnergy)
            val percentUsed = (usedEnergy * 100 / maxCapacity).toInt()
            user.sendMessage(
                net.minecraft.text.Text.literal("§a已充能 $itemsCharged 件物品，消耗 §e$formattedEnergy EU§a (${percentUsed}%)"),
                true
            )
        } else {
            user.sendMessage(net.minecraft.text.Text.literal("§c没有物品需要充电！"), true)
        }

        return net.minecraft.util.TypedActionResult.success(crystal)
    }
}

// ========== 无线充电电池 ==========

/**
 * 无线充电电池（等级1）
 *
 * @spec 等级1, 容量40,000 EU, 速度32 EU/t
 */
@ModItem(name = "re_battery_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class ReBatteryWirelessItem : WirelessBatteryItemBase(
    name = "re_battery_wireless", tier = 1, baseMaxCapacity = 10_000
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 四角电路板，四边充电电池，中心空
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = ReBatteryWirelessItem::class.recipeId(),
                result = ReBatteryWirelessItem::class.instance(),
                pattern = listOf("CRC", "R R", "CRC"),
                keys = mapOf(
                    'C' to Circuit::class.instance(),
                    'R' to ReBatteryItem::class.instance()
                )
            )
        }
    }
}

/**
 * 高级无线充电电池（等级2）
 *
 * @spec 等级2, 容量400,000 EU, 速度128 EU/t
 */
@ModItem(name = "advanced_re_battery_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class AdvancedReBatteryWirelessItem : WirelessBatteryItemBase(
    name = "advanced_re_battery_wireless", tier = 2, baseMaxCapacity = 100_000
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 四角热交换器，四边高级充电电池，中心无线充电电池（等级1）
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = AdvancedReBatteryWirelessItem::class.recipeId(),
                result = AdvancedReBatteryWirelessItem::class.instance(),
                pattern = listOf("HAH", "AWA", "HAH"),
                keys = mapOf(
                    'H' to HeatExchangerItem::class.instance(),
                    'A' to AdvancedReBatteryItem::class.instance(),
                    'W' to ReBatteryWirelessItem::class.instance()
                )
            )
        }
    }
}

/**
 * 无线能量水晶（等级3）
 *
 * @spec 等级3, 容量4,000,000 EU, 速度512 EU/t
 */
@ModItem(name = "energy_crystal_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class EnergyCrystalWirelessItem : WirelessBatteryItemBase(
    name = "energy_crystal_wireless", tier = 3, baseMaxCapacity = 1_000_000
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 四角元件热交换器，四边能量水晶，中心高级无线充电电池
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = EnergyCrystalWirelessItem::class.recipeId(),
                result = EnergyCrystalWirelessItem::class.instance(),
                pattern = listOf("CEC", "EWE", "CEC"),
                keys = mapOf(
                    'C' to ComponentHeatExchangerItem::class.instance(),
                    'E' to EnergyCrystalItem::class.instance(),
                    'W' to AdvancedReBatteryWirelessItem::class.instance()
                )
            )
        }
    }
}

/**
 * 无线蓝波顿水晶（等级4）
 *
 * @spec 等级4, 容量40,000,000 EU, 速度2,048 EU/t
 */
@ModItem(name = "lapotron_crystal_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class LapotronCrystalWirelessItem : WirelessBatteryItemBase(
    name = "lapotron_crystal_wireless", tier = 4, baseMaxCapacity = 10_000_000
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 四角高级热交换器，四边兰波顿水晶，中心无线能量水晶
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LapotronCrystalWirelessItem::class.recipeId(),
                result = LapotronCrystalWirelessItem::class.instance(),
                pattern = listOf("ALA", "LWL", "ALA"),
                keys = mapOf(
                    'A' to AdvancedHeatExchangerItem::class.instance(),
                    'L' to LapotronCrystalItem::class.instance(),
                    'W' to EnergyCrystalWirelessItem::class.instance()
                )
            )
        }
    }
}

// ========== 特殊电池 ==========

/**
 * 一次性电池（Single Use Battery）
 *
 * 只能放电，不能充电。电量耗尽后自动销毁。
 *
 * @spec 等级1, 容量10,000 EU, 速度32 EU/t, 不可充电
 */
@ModItem(name = "single_use_battery", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class SingleUseBatteryItem : BatteryItemBase(
    name = "single_use_battery", tier = 1, maxCapacity = 10_000, canChargeWireless = false
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val copperCable = InsulatedCopperCableBlock::class.item()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SingleUseBatteryItem::class.instance(), 8)
                .pattern(" W ")
                .pattern(" R ")
                .pattern(" F ")
                .input('W', copperCable)
                .input('R', Items.REDSTONE)
                .input('F', CoalFuelDust::class.instance())
                .criterion(hasItem(copperCable), conditionsFromItem(copperCable))
                .offerTo(exporter, SingleUseBatteryItem::class.recipeId())
        }
    }

    override val canCharge: Boolean get() = false
    override fun discharge(stack: net.minecraft.item.ItemStack, amount: Long): Long {
        val discharged = super.discharge(stack, amount)

        // 电量耗尽后销毁物品
        if (isEmpty(stack)) {
            stack.count = 0 // 清空物品栈
        }

        return discharged
    }

    override fun charge(stack: net.minecraft.item.ItemStack, amount: Long): Long {
        // 一次性电池不能充电
        return 0
    }
}

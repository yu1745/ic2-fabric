package ic2_120.content.item.energy

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

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
    name = "re_battery", tier = 1, maxCapacity = 10_000, transferSpeed = 32, canChargeWireless = false
)

/**
 * 高级充电电池（Advanced Re-Battery）
 *
 * @spec 等级2, 容量100,000 EU, 速度128 EU/t
 */
@ModItem(name = "advanced_re_battery", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class AdvancedReBatteryItem : BatteryItemBase(
    name = "advanced_re_battery", tier = 2, maxCapacity = 100_000, transferSpeed = 128, canChargeWireless = false
)

/**
 * 能量水晶（Energy Crystal）
 *
 * @spec 等级3, 容量1,000,000 EU, 速度512 EU/t
 */
@ModItem(name = "energy_crystal", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class EnergyCrystalItem : BatteryItemBase(
    name = "energy_crystal", tier = 3, maxCapacity = 1_000_000, transferSpeed = 512, canChargeWireless = false
)

/**
 * 蓝波顿水晶（Lapotron Crystal）
 *
 * @spec 等级4, 容量10,000,000 EU, 速度2,048 EU/t
 */
@ModItem(name = "lapotron_crystal", tab = CreativeTab.IC2_MATERIALS, group = "battery")
class LapotronCrystalItem : BatteryItemBase(
    name = "lapotron_crystal", tier = 4, maxCapacity = 10_000_000, transferSpeed = 2_048, canChargeWireless = false
)

// ========== 无线充电电池 ==========

/**
 * 无线充电电池（等级1）
 *
 * @spec 等级1, 容量40,000 EU, 速度32 EU/t
 */
@ModItem(name = "re_battery_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class ReBatteryWirelessItem : WirelessBatteryItemBase(
    name = "re_battery_wireless", tier = 1, baseMaxCapacity = 10_000, transferSpeed = 32
)

/**
 * 高级无线充电电池（等级2）
 *
 * @spec 等级2, 容量400,000 EU, 速度128 EU/t
 */
@ModItem(name = "advanced_re_battery_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class AdvancedReBatteryWirelessItem : WirelessBatteryItemBase(
    name = "advanced_re_battery_wireless", tier = 2, baseMaxCapacity = 100_000, transferSpeed = 128
)

/**
 * 无线能量水晶（等级3）
 *
 * @spec 等级3, 容量4,000,000 EU, 速度512 EU/t
 */
@ModItem(name = "energy_crystal_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class EnergyCrystalWirelessItem : WirelessBatteryItemBase(
    name = "energy_crystal_wireless", tier = 3, baseMaxCapacity = 1_000_000, transferSpeed = 512
)

/**
 * 无线蓝波顿水晶（等级4）
 *
 * @spec 等级4, 容量40,000,000 EU, 速度2,048 EU/t
 */
@ModItem(name = "lapotron_crystal_wireless", tab = CreativeTab.IC2_MATERIALS, group = "battery_wireless")
class LapotronCrystalWirelessItem : WirelessBatteryItemBase(
    name = "lapotron_crystal_wireless", tier = 4, baseMaxCapacity = 10_000_000, transferSpeed = 2_048
)

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
    name = "single_use_battery", tier = 1, maxCapacity = 10_000, transferSpeed = 32, canChargeWireless = false
) {
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

package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item

// ========== 升级物品接口 ==========

/**
 * 机器升级物品标记接口。
 * 实现此接口的物品可放入机器的升级槽位。
 * 各升级的实际效果由机器在 tick 中自行读取并应用，此处不定义。
 */
interface IUpgradeItem

// ========== 工具升级类 ==========

@ModItem(name = "overclocker_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class OverclockerUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "transformer_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class TransformerUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "energy_storage_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EnergyStorageUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "redstone_inverter_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
// 红石反转升级（效果待实现）
class RedstoneInverterUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
// 弹出升级（效果待实现）
class EjectorUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "advanced_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
// 自动弹出升级（效果待实现）
class AdvancedEjectorUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
// 抽入升级（效果待实现）
class PullingUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "advanced_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
// 自动抽入升级（效果待实现）
class AdvancedPullingUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "fluid_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
// 流体弹出升级（效果待实现）
class FluidEjectorUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "fluid_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
// 流体抽入升级（效果待实现）
class FluidPullingUpgrade : Item(FabricItemSettings()), IUpgradeItem

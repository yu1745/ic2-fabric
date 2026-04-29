package ic2_120.content.tab

import ic2_120.registry.annotation.ModCreativeTab
import ic2_120.registry.type

/**
 * IC2 材料物品栏。
 * 包含所有金属、橡胶等基础材料。
 */
@ModCreativeTab(name = "ic2_materials", iconItem = "tin_ingot")
class Ic2MaterialsTab

/**
 * IC2 机器物品栏。
 * 包含所有机器、发电机等设备。
 */
@ModCreativeTab(name = "ic2_machines", iconItem = "electric_furnace")
class Ic2MachinesTab

/**
 * IC2 工具物品栏。
 * 包含采矿镭射枪、扳手、钻头等工具。
 *
 * 图标使用 [ModCreativeTab.iconResource] 指向贴图路径，由占位物品渲染，避免用采矿镭射枪物品导致 EU 条。
 */
@ModCreativeTab(name = "ic2_tools", iconResource = "ic2:item/tool/electric/mining_laser")
class Ic2ToolsTab

/**
 * IC2 作物种子物品栏。
 * 包含杂交系统的种子袋与预制初始种子。
 */
@ModCreativeTab(name = "ic2_crop_seeds", iconItem = "crop_seed_bag")
class Ic2CropSeedsTab

package ic2_120.content.item

import ic2_120.content.entity.ModEntities
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

/** 破损的橡胶船 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "broken_rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class BrokenRubberBoatItem : Ic2BoatItem(ModEntities.BROKEN_RUBBER_BOAT, FabricItemSettings())

/** 碳纤维船 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "carbon_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class CarbonBoatItem : Ic2BoatItem(ModEntities.CARBON_BOAT, FabricItemSettings())

/** 橡皮艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class RubberBoatItem : Ic2BoatItem(ModEntities.RUBBER_BOAT, FabricItemSettings())

/** 电动艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "electric_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class ElectricBoatItem : Ic2BoatItem(ModEntities.ELECTRIC_BOAT, FabricItemSettings())

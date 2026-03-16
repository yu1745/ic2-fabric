package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// ========== 核能相关材料 ==========

/** 浓缩铀核燃料 */
@ModItem(name = "uranium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium : Item(FabricItemSettings())

/** 铀 -235 */
@ModItem(name = "uranium_235", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium235 : Item(FabricItemSettings())

/** 铀 -238 */
@ModItem(name = "uranium_238", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium238 : Item(FabricItemSettings())

/** 钚 */
@ModItem(name = "plutonium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Plutonium : Item(FabricItemSettings())

/** 钚铀混合氧化物核燃料 (MOX) */
@ModItem(name = "mox", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Mox : Item(FabricItemSettings())

/** 小撮铀 -235 */
@ModItem(name = "small_uranium_235", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallUranium235 : Item(FabricItemSettings())

/** 小撮铀 -238 */
@ModItem(name = "small_uranium_238", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallUranium238 : Item(FabricItemSettings())

/** 小撮钚 */
@ModItem(name = "small_plutonium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallPlutonium : Item(FabricItemSettings())

/** 浓缩铀核燃料靶丸 */
@ModItem(name = "uranium_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class UraniumPellet : Item(FabricItemSettings())

/** 钚铀混合氧化物核燃料靶丸 (MOX) */
@ModItem(name = "mox_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class MoxPellet : Item(FabricItemSettings())

/** 放射性同位素燃料靶丸 */
@ModItem(name = "rtg_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class RtgPellet : Item(FabricItemSettings())

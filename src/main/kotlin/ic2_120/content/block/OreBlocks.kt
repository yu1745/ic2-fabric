package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks

// ========== 普通矿石（石头基质） ==========

/** 铅矿石（生成参数照搬原版金矿：Y -64~32，矿脉小） */
@ModBlock(name = "lead_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class LeadOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE)
)

/** 锡矿石（生成参数照搬原版铁矿：Y -64~256，矿脉大） */
@ModBlock(name = "tin_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class TinOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE)
)

/** 铀矿石（生成参数照搬原版钻石矿：Y -64~16，稀有） */
@ModBlock(name = "uranium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class UraniumOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE)
)

// 铜矿石：原版已有 minecraft:copper_ore，此处不再注册
// @ModBlock(name = "copper_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
// class CopperOreBlock : Block(
//     AbstractBlock.Settings.copy(Blocks.IRON_ORE).strength(3.0f, 3.0f)
// )

/** 铱矿石 */
@ModBlock(name = "iridium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class IridiumOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE)
)

// ========== 深层矿石（深板岩基质） ==========

/** 深层铅矿石（照搬原版深层金矿） */
@ModBlock(name = "deepslate_lead_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class DeepslateLeadOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE)
)

/** 深层锡矿石（照搬原版深层铁矿） */
@ModBlock(name = "deepslate_tin_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class DeepslateTinOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE)
)

/** 深层铀矿石（照搬原版深层钻石矿） */
@ModBlock(name = "deepslate_uranium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class DeepslateUraniumOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE)
)

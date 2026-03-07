package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks

// ========== 普通矿石（石头基质） ==========

/** 铅矿石 */
@ModBlock(name = "lead_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class LeadOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE).strength(3.0f, 3.0f)
)

/** 锡矿石 */
@ModBlock(name = "tin_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class TinOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE).strength(3.0f, 3.0f)
)

/** 铀矿石 */
@ModBlock(name = "uranium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class UraniumOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE).strength(3.0f, 3.0f)
)

/** 铜矿石 */
@ModBlock(name = "copper_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class CopperOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE).strength(3.0f, 3.0f)
)

/** 铱矿石 */
@ModBlock(name = "iridium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class IridiumOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_ORE).strength(3.0f, 3.0f)
)

// ========== 深层矿石（深板岩基质） ==========

/** 深层铅矿石 */
@ModBlock(name = "deepslate_lead_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class DeepslateLeadOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.DEEPSLATE_IRON_ORE).strength(4.5f, 3.0f)
)

/** 深层锡矿石 */
@ModBlock(name = "deepslate_tin_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class DeepslateTinOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.DEEPSLATE_IRON_ORE).strength(4.5f, 3.0f)
)

/** 深层铀矿石 */
@ModBlock(name = "deepslate_uranium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class DeepslateUraniumOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.DEEPSLATE_IRON_ORE).strength(4.5f, 3.0f)
)

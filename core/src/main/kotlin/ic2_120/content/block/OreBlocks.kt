package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.block.SoundType

// ========== 普通矿石（石头基质） ==========

/** 铅矿石（生成参数照搬原版金矿：Y -64~32，矿脉小） */
@ModBlock(name = "lead_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS, materialTags = ["ores/lead"])
class LeadOreBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.IRON_ORE)
)

/** 锡矿石（生成参数照搬原版铁矿：Y -64~256，矿脉大） */
@ModBlock(name = "tin_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS, materialTags = ["ores/tin"])
class TinOreBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.IRON_ORE)
)

/** 泥炭矿 */
@ModBlock(name = "peat_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class PeatOreBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.IRON_ORE)
)

/** 铀矿石（生成参数照搬原版钻石矿：Y -64~16，稀有） */
@ModBlock(name = "uranium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS, materialTags = ["ores/uranium"])
class UraniumOreBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.IRON_ORE)
)

// 铜矿石：原版已有 minecraft:copper_ore，此处不再注册
// @ModBlock(name = "copper_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
// class CopperOreBlock : Block(
//     BlockBehaviour.Settings.copy(Blocks.IRON_ORE).strength(3.0f, 3.0f)
// )

// ========== 深层矿石（深板岩基质） ==========

/** 深层铅矿石（照搬原版深层金矿） */
@ModBlock(name = "deepslate_lead_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS, materialTags = ["ores/lead"])
class DeepslateLeadOreBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.IRON_ORE)
        .mapColor(MapColor.DEEPSLATE_GRAY)
        .strength(4.5f, 3.0f)
        .sounds(SoundType.DEEPSLATE)
)

/** 深层锡矿石（照搬原版深层铁矿） */
@ModBlock(name = "deepslate_tin_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS, materialTags = ["ores/tin"])
class DeepslateTinOreBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.IRON_ORE)
        .mapColor(MapColor.DEEPSLATE_GRAY)
        .strength(4.5f, 3.0f)
        .sounds(SoundType.DEEPSLATE)
)

/** 深层铀矿石（照搬原版深层钻石矿） */
@ModBlock(name = "deepslate_uranium_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS, materialTags = ["ores/uranium"])
class DeepslateUraniumOreBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.IRON_ORE)
        .mapColor(MapColor.DEEPSLATE_GRAY)
        .strength(4.5f, 3.0f)
        .sounds(SoundType.DEEPSLATE)
)

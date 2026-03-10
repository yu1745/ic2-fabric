package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.*
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.gen.feature.ConfiguredFeature

// ========== 原木 / 木材 ==========

@ModBlock(name = "rubber_log", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberLogBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_LOG).strength(2.0f)) : PillarBlock(settings)

@ModBlock(name = "stripped_rubber_log", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class StrippedRubberLogBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_LOG).strength(2.0f)) : PillarBlock(settings)

@ModBlock(name = "stripped_rubber_wood", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class StrippedRubberWoodBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.0f)) : PillarBlock(settings)

@ModBlock(name = "rubber_planks", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberPlanksBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0f)) : Block(settings)

// ========== 台阶 / 楼梯 ==========

@ModBlock(name = "rubber_slab", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberSlabBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SLAB).strength(2.0f)) : SlabBlock(settings)

@ModBlock(name = "rubber_stairs", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberStairsBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_STAIRS).strength(2.0f)) : StairsBlock(Blocks.OAK_PLANKS.defaultState, settings)

// ========== 栅栏 / 栅栏门 ==========

@ModBlock(name = "rubber_fence", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberFenceBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_FENCE).strength(2.0f)) : FenceBlock(settings)

@ModBlock(name = "rubber_fence_gate", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberFenceGateBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).strength(2.0f)) : FenceGateBlock(settings, WoodType.OAK)

// ========== 门 / 活板门 ==========

@ModBlock(name = "rubber_door", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberDoorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_DOOR).strength(2.0f)) : DoorBlock(settings, BlockSetType.OAK)

@ModBlock(name = "rubber_trapdoor", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberTrapdoorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).strength(2.0f)) : TrapdoorBlock(settings, BlockSetType.OAK)

// ========== 按钮 / 压力板 ==========

@ModBlock(name = "rubber_button", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberButtonBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_BUTTON).strength(2.0f)) : ButtonBlock(settings, BlockSetType.OAK, 30, true)

@ModBlock(name = "rubber_pressure_plate", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberPressurePlateBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE).strength(2.0f)) : PressurePlateBlock(PressurePlateBlock.ActivationRule.EVERYTHING, settings, BlockSetType.OAK)

// ========== 树叶 / 树苗 ==========

/** 橡胶树生成器，用于树苗生长与骨粉催熟。 */
private class RubberSaplingGenerator : net.minecraft.block.sapling.SaplingGenerator() {
    override fun getTreeFeature(random: net.minecraft.util.math.random.Random, bees: Boolean): RegistryKey<ConfiguredFeature<*, *>>? =
        RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier(Ic2_120.MOD_ID, "rubber_tree"))
}

@ModBlock(name = "rubber_leaves", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
class RubberLeavesBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).strength(0.2f)) : LeavesBlock(settings)

/** 橡胶树苗，支持骨粉催熟与自然生长。 */
@ModBlock(name = "rubber_sapling", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood", transparent = true)
class RubberSaplingBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SAPLING).strength(0.0f)
) : SaplingBlock(RubberSaplingGenerator(), settings)

// ========== 告示牌 ==========

//todo 材质有点问题，先禁用
// @ModBlock(name = "rubber_sign", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wood")
// class RubberSignBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_SIGN).strength(1.0f).nonOpaque()) : net.minecraft.block.SignBlock(settings, net.minecraft.block.WoodType.OAK)

// @ModBlock(name = "rubber_wall_sign", registerItem = false, tab = CreativeTab.MINECRAFT_DECORATIONS, group = "wood")
// class RubberWallSignBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_WALL_SIGN).strength(1.0f).nonOpaque().dropsLike(Blocks.OAK_SIGN)) : net.minecraft.block.WallSignBlock(settings, net.minecraft.block.WoodType.OAK)

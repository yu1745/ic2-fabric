package ic2_120.content.recipes.macerator

import ic2_120.content.block.DeepslateLeadOreBlock
import ic2_120.content.block.DeepslateTinOreBlock
import ic2_120.content.block.DeepslateUraniumOreBlock
import ic2_120.content.block.LeadOreBlock
import ic2_120.content.block.TinOreBlock
import ic2_120.content.block.UraniumOreBlock
import ic2_120.content.item.BioChaff
import ic2_120.content.item.BronzeDust
import ic2_120.content.item.BronzeIngot
import ic2_120.content.item.BronzePlate
import ic2_120.content.item.ClayDust
import ic2_120.content.item.CoalDust
import ic2_120.content.item.CopperDust
import ic2_120.content.item.CopperPlate
import ic2_120.content.item.CrushedCopper
import ic2_120.content.item.CrushedGold
import ic2_120.content.item.CrushedIron
import ic2_120.content.item.CrushedLead
import ic2_120.content.item.CrushedTin
import ic2_120.content.item.CrushedUranium
import ic2_120.content.item.DiamondDust
import ic2_120.content.item.GoldDust
import ic2_120.content.item.GoldPlate
import ic2_120.content.item.GrinPowder
import ic2_120.content.item.IronDust
import ic2_120.content.item.IronPlate
import ic2_120.content.item.LapisDust
import ic2_120.content.item.LapisPlate
import ic2_120.content.item.LeadDust
import ic2_120.content.item.LeadIngot
import ic2_120.content.item.LeadPlate
import ic2_120.content.item.NetherrackDust
import ic2_120.content.item.ObsidianDust
import ic2_120.content.item.ObsidianPlate
import ic2_120.content.item.PlantBall
import ic2_120.content.item.RawLead
import ic2_120.content.item.RawTin
import ic2_120.content.item.RawUranium
import ic2_120.content.item.SilverDust
import ic2_120.content.item.SilverIngot
import ic2_120.content.item.SmallObsidianDust
import ic2_120.content.item.TinDust
import ic2_120.content.item.TinIngot
import ic2_120.content.item.TinPlate
import ic2_120.content.item.Weed
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.util.Identifier

object MaceratorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: Item,
        val output: Item,
        val count: Int,
        val inputCount: Int = 1
    )

    private val entries = listOf(
        Entry("iron_ore_to_crushed_iron", Items.IRON_ORE, CrushedIron::class.instance(), 2),
        Entry("gold_ore_to_crushed_gold", Items.GOLD_ORE, CrushedGold::class.instance(), 2),
        Entry("copper_ore_to_crushed_copper", Items.COPPER_ORE, CrushedCopper::class.instance(), 2),
        Entry("lead_ore_to_crushed_lead", LeadOreBlock::class.item(), CrushedLead::class.instance(), 2),
        Entry("tin_ore_to_crushed_tin", TinOreBlock::class.item(), CrushedTin::class.instance(), 2),
        Entry("uranium_ore_to_crushed_uranium", UraniumOreBlock::class.item(), CrushedUranium::class.instance(), 2),
        Entry("deepslate_iron_ore_to_crushed_iron", Items.DEEPSLATE_IRON_ORE, CrushedIron::class.instance(), 2),
        Entry("deepslate_gold_ore_to_crushed_gold", Items.DEEPSLATE_GOLD_ORE, CrushedGold::class.instance(), 2),
        Entry("deepslate_copper_ore_to_crushed_copper", Items.DEEPSLATE_COPPER_ORE, CrushedCopper::class.instance(), 2),
        Entry("deepslate_lead_ore_to_crushed_lead", DeepslateLeadOreBlock::class.item(), CrushedLead::class.instance(), 2),
        Entry("deepslate_tin_ore_to_crushed_tin", DeepslateTinOreBlock::class.item(), CrushedTin::class.instance(), 2),
        Entry("deepslate_uranium_ore_to_crushed_uranium", DeepslateUraniumOreBlock::class.item(), CrushedUranium::class.instance(), 2),
        Entry("cobblestone_to_gravel", Items.COBBLESTONE, Items.GRAVEL, 1),
        Entry("gravel_to_flint", Items.GRAVEL, Items.FLINT, 1),
        Entry("stone_to_cobblestone", Items.STONE, Items.COBBLESTONE, 1),
        Entry("granite_to_cobblestone", Items.GRANITE, Items.COBBLESTONE, 1),
        Entry("diorite_to_cobblestone", Items.DIORITE, Items.COBBLESTONE, 1),
        Entry("andesite_to_cobblestone", Items.ANDESITE, Items.COBBLESTONE, 1),
        Entry("netherrack_to_netherrack_dust", Items.NETHERRACK, NetherrackDust::class.instance(), 1),
        Entry("obsidian_to_obsidian_dust", Items.OBSIDIAN, ObsidianDust::class.instance(), 1),
        Entry("obsidian_plate_to_small_obsidian_dust", ObsidianPlate::class.instance(), SmallObsidianDust::class.instance(), 8),
        Entry("coal_to_coal_dust", Items.COAL, CoalDust::class.instance(), 1),
        Entry("coal_ore_to_coal_dust", Items.COAL_ORE, CoalDust::class.instance(), 2),
        Entry("deepslate_coal_ore_to_coal_dust", Items.DEEPSLATE_COAL_ORE, CoalDust::class.instance(), 2),
        Entry("coal_block_to_coal_dust", Items.COAL_BLOCK, CoalDust::class.instance(), 9),
        Entry("clay_block_to_clay_dust", Items.CLAY, ClayDust::class.instance(), 2),
        // 植物打粉配方
        Entry("plant_ball_to_bio_chaff", PlantBall::class.instance(), BioChaff::class.instance(), 1),
        Entry("pumpkin_to_bio_chaff", Items.PUMPKIN, BioChaff::class.instance(), 1, 8),
        Entry("melon_slice_to_bio_chaff", Items.MELON_SLICE, BioChaff::class.instance(), 1, 8),
        Entry("melon_seeds_to_bio_chaff", Items.MELON_SEEDS, BioChaff::class.instance(), 1, 16),
        Entry("pumpkin_seeds_to_bio_chaff", Items.PUMPKIN_SEEDS, BioChaff::class.instance(), 1, 16),
        Entry("wheat_seeds_to_bio_chaff", Items.WHEAT_SEEDS, BioChaff::class.instance(), 1, 16),
        Entry("wheat_to_bio_chaff", Items.WHEAT, BioChaff::class.instance(), 1, 8),
        Entry("carrot_to_bio_chaff", Items.CARROT, BioChaff::class.instance(), 1, 8),
        Entry("potato_to_bio_chaff", Items.POTATO, BioChaff::class.instance(), 1, 8),
        Entry("poisonous_potato_to_grin_powder", Items.POISONOUS_POTATO, GrinPowder::class.instance(), 1),
        Entry("oak_sapling_to_bio_chaff", Items.OAK_SAPLING, BioChaff::class.instance(), 1, 4),
        Entry("cactus_to_bio_chaff", Items.CACTUS, BioChaff::class.instance(), 1, 8),
        Entry("sugar_cane_to_bio_chaff", Items.SUGAR_CANE, BioChaff::class.instance(), 1, 8),
        Entry("oak_leaves_to_bio_chaff", Items.OAK_LEAVES, BioChaff::class.instance(), 1, 8),
        Entry("dead_bush_to_bio_chaff", Items.DEAD_BUSH, BioChaff::class.instance(), 1, 8),
        Entry("weed_to_bio_chaff", Weed::class.instance(), BioChaff::class.instance(), 1, 32),
        Entry("diamond_to_diamond_dust", Items.DIAMOND, DiamondDust::class.instance(), 1),
        Entry("raw_iron_to_crushed_iron", Items.RAW_IRON, CrushedIron::class.instance(), 2),
        Entry("raw_gold_to_crushed_gold", Items.RAW_GOLD, CrushedGold::class.instance(), 2),
        Entry("raw_copper_to_crushed_copper", Items.RAW_COPPER, CrushedCopper::class.instance(), 2),
        Entry("raw_lead_to_crushed_lead", RawLead::class.instance(), CrushedLead::class.instance(), 2),
        Entry("raw_tin_to_crushed_tin", RawTin::class.instance(), CrushedTin::class.instance(), 2),
        Entry("raw_uranium_to_crushed_uranium", RawUranium::class.instance(), CrushedUranium::class.instance(), 2),
        // 金属锭 / 板 → 粉
        Entry("copper_ingot_to_copper_dust", Items.COPPER_INGOT, CopperDust::class.instance(), 1),
        Entry("tin_ingot_to_tin_dust", TinIngot::class.instance(), TinDust::class.instance(), 1),
        Entry("bronze_ingot_to_bronze_dust", BronzeIngot::class.instance(), BronzeDust::class.instance(), 1),
        Entry("gold_ingot_to_gold_dust", Items.GOLD_INGOT, GoldDust::class.instance(), 1),
        Entry("iron_ingot_to_iron_dust", Items.IRON_INGOT, IronDust::class.instance(), 1),
        Entry("lead_ingot_to_lead_dust", LeadIngot::class.instance(), LeadDust::class.instance(), 1),
        Entry("silver_ingot_to_silver_dust", SilverIngot::class.instance(), SilverDust::class.instance(), 1),
        Entry("lapis_to_lapis_dust", Items.LAPIS_LAZULI, LapisDust::class.instance(), 1),
        Entry("copper_plate_to_copper_dust", CopperPlate::class.instance(), CopperDust::class.instance(), 1),
        Entry("tin_plate_to_tin_dust", TinPlate::class.instance(), TinDust::class.instance(), 1),
        Entry("bronze_plate_to_bronze_dust", BronzePlate::class.instance(), BronzeDust::class.instance(), 1),
        Entry("gold_plate_to_gold_dust", GoldPlate::class.instance(), GoldDust::class.instance(), 1),
        Entry("iron_plate_to_iron_dust", IronPlate::class.instance(), IronDust::class.instance(), 1),
        Entry("lead_plate_to_lead_dust", LeadPlate::class.instance(), LeadDust::class.instance(), 1),
        Entry("lapis_plate_to_lapis_dust", LapisPlate::class.instance(), LapisDust::class.instance(), 1)
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeExporter) {
        entries.forEach { entry ->
            val id = Identifier.of("ic2_120", "macerating/${entry.name}")
            val recipe = MaceratorRecipe(
                id = id,
                ingredient = Ingredient.ofItems(entry.input),
                output = ItemStack(entry.output, entry.count)
            )
            exporter.accept(id, recipe, null)
        }
    }
}

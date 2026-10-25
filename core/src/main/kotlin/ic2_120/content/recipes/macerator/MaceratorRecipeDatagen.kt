package ic2_120.content.recipes.macerator

import ic2_120.compat.accept

import ic2_120.content.recipes.IngredientInput
import ic2_120.content.recipes.ModMachineRecipes
import ic2_120.content.recipes.ModTags
import ic2_120.content.block.LeadOreBlock
import ic2_120.content.block.TinOreBlock
import ic2_120.content.block.UraniumOreBlock
import ic2_120.registry.item
import ic2_120.content.item.BronzeDust
import ic2_120.content.item.BronzeIngot
import ic2_120.content.item.BronzePlate
import ic2_120.content.item.CopperDust
import ic2_120.content.item.CopperPlate
import ic2_120.content.item.CrushedCopper
import ic2_120.content.item.CrushedGold
import ic2_120.content.item.CrushedIron
import ic2_120.content.item.RawLead
import ic2_120.content.item.RawTin
import ic2_120.content.item.RawUranium
import ic2_120.content.item.CrushedLead
import ic2_120.content.item.CrushedTin
import ic2_120.content.item.CrushedUranium
import ic2_120.content.item.GoldDust
import ic2_120.content.item.GoldPlate
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
import ic2_120.content.item.SilverDust
import ic2_120.content.item.SilverIngot
import ic2_120.content.item.SmallObsidianDust
import ic2_120.content.item.TinDust
import ic2_120.content.item.TinIngot
import ic2_120.content.item.TinPlate
import ic2_120.content.item.PlantBall
import ic2_120.content.item.BioChaff
import ic2_120.content.item.Weed
import ic2_120.content.item.CoalDust
import ic2_120.content.item.ClayDust
import ic2_120.content.item.DiamondDust
import ic2_120.content.item.GrinPowder
import ic2_120.registry.instance
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.ItemTags
import net.minecraft.resources.Identifier

object MaceratorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: IngredientInput,
        val output: Item,
        val count: Int,
        val inputCount: Int = 1
    )

    /** 外部模组配方：直接写物品 ID，避免 AE2 未加载时通过 BuiltInRegistries 得到 air。 */
    private data class ExternalEntry(
        val name: String,
        val input: Identifier,
        val output: Identifier,
        val count: Int = 1,
        val inputCount: Int = 1
    )

    private val entries = listOf(
        // 矿石（tag 包含 deepslate 变体）
        Entry("iron_ore_to_crushed_iron", IngredientInput.tag(ModTags.Compat.Items.ORES_IRON, Items.IRON_ORE), CrushedIron::class.instance(), 2),
        Entry("gold_ore_to_crushed_gold", IngredientInput.tag(ModTags.Compat.Items.ORES_GOLD, Items.GOLD_ORE), CrushedGold::class.instance(), 2),
        Entry("copper_ore_to_crushed_copper", IngredientInput.tag(ModTags.Compat.Items.ORES_COPPER, Items.COPPER_ORE), CrushedCopper::class.instance(), 2),
        Entry("lead_ore_to_crushed_lead", IngredientInput.tag(ModTags.Compat.Items.ORES_LEAD, LeadOreBlock::class.item()), CrushedLead::class.instance(), 2),
        Entry("tin_ore_to_crushed_tin", IngredientInput.tag(ModTags.Compat.Items.ORES_TIN, TinOreBlock::class.item()), CrushedTin::class.instance(), 2),
        Entry("uranium_ore_to_crushed_uranium", IngredientInput.tag(ModTags.Compat.Items.ORES_URANIUM, UraniumOreBlock::class.item()), CrushedUranium::class.instance(), 2),
        Entry("coal_ore_to_coal_dust", IngredientInput.tag(ModTags.Compat.Items.ORES_COAL, Items.COAL_ORE), CoalDust::class.instance(), 2),
        // 原矿
        Entry("raw_iron_to_crushed_iron", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_IRON, Items.RAW_IRON), CrushedIron::class.instance(), 2),
        Entry("raw_gold_to_crushed_gold", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_GOLD, Items.RAW_GOLD), CrushedGold::class.instance(), 2),
        Entry("raw_copper_to_crushed_copper", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_COPPER, Items.RAW_COPPER), CrushedCopper::class.instance(), 2),
        Entry("raw_lead_to_crushed_lead", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_LEAD, RawLead::class.instance()), CrushedLead::class.instance(), 2),
        Entry("raw_tin_to_crushed_tin", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_TIN, RawTin::class.instance()), CrushedTin::class.instance(), 2),
        Entry("raw_uranium_to_crushed_uranium", IngredientInput.tag(ModTags.Compat.Items.RAW_MATERIALS_URANIUM, RawUranium::class.instance()), CrushedUranium::class.instance(), 2),
        // 石头类（保留为 Item）
        Entry("cobblestone_to_gravel", IngredientInput.item(Items.COBBLESTONE), Items.GRAVEL, 1),
        Entry("gravel_to_flint", IngredientInput.item(Items.GRAVEL), Items.FLINT, 1),
        Entry("stone_to_cobblestone", IngredientInput.item(Items.STONE), Items.COBBLESTONE, 1),
        Entry("granite_to_cobblestone", IngredientInput.item(Items.GRANITE), Items.COBBLESTONE, 1),
        Entry("diorite_to_cobblestone", IngredientInput.item(Items.DIORITE), Items.COBBLESTONE, 1),
        Entry("andesite_to_cobblestone", IngredientInput.item(Items.ANDESITE), Items.COBBLESTONE, 1),
        Entry("netherrack_to_netherrack_dust", IngredientInput.item(Items.NETHERRACK), NetherrackDust::class.instance(), 1),
        Entry("obsidian_to_obsidian_dust", IngredientInput.item(Items.OBSIDIAN), ObsidianDust::class.instance(), 1),
        Entry("obsidian_plate_to_small_obsidian_dust", IngredientInput.item(ObsidianPlate::class.instance()), SmallObsidianDust::class.instance(), 8),
        Entry("blaze_rod_to_blaze_powder", IngredientInput.item(Items.BLAZE_ROD), Items.BLAZE_POWDER, 5),
        Entry("coal_to_coal_dust", IngredientInput.item(Items.COAL), CoalDust::class.instance(), 1),
        Entry("coal_block_to_coal_dust", IngredientInput.item(Items.COAL_BLOCK), CoalDust::class.instance(), 9),
        Entry("clay_block_to_clay_dust", IngredientInput.item(Items.CLAY), ClayDust::class.instance(), 2),
        Entry("wool_to_string", IngredientInput.tag(ItemTags.WOOL, Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE)), Items.STRING, 4),
        // 植物打粉配方
        Entry("plant_ball_to_bio_chaff", IngredientInput.item(PlantBall::class.instance()), BioChaff::class.instance(), 1),
        Entry("pumpkin_to_bio_chaff", IngredientInput.item(Items.PUMPKIN), BioChaff::class.instance(), 1, 8),
        Entry("melon_slice_to_bio_chaff", IngredientInput.item(Items.MELON_SLICE), BioChaff::class.instance(), 1, 8),
        Entry("melon_seeds_to_bio_chaff", IngredientInput.item(Items.MELON_SEEDS), BioChaff::class.instance(), 1, 16),
        Entry("pumpkin_seeds_to_bio_chaff", IngredientInput.item(Items.PUMPKIN_SEEDS), BioChaff::class.instance(), 1, 16),
        Entry("wheat_seeds_to_bio_chaff", IngredientInput.item(Items.WHEAT_SEEDS), BioChaff::class.instance(), 1, 16),
        Entry("wheat_to_bio_chaff", IngredientInput.item(Items.WHEAT), BioChaff::class.instance(), 1, 8),
        Entry("carrot_to_bio_chaff", IngredientInput.item(Items.CARROT), BioChaff::class.instance(), 1, 8),
        Entry("potato_to_bio_chaff", IngredientInput.item(Items.POTATO), BioChaff::class.instance(), 1, 8),
        Entry("poisonous_potato_to_grin_powder", IngredientInput.item(Items.POISONOUS_POTATO), GrinPowder::class.instance(), 1),
        Entry("spider_eye_to_grin_powder", IngredientInput.item(Items.SPIDER_EYE), GrinPowder::class.instance(), 1),
        Entry("oak_sapling_to_bio_chaff", IngredientInput.item(Items.OAK_SAPLING), BioChaff::class.instance(), 1, 4),
        Entry("cactus_to_bio_chaff", IngredientInput.item(Items.CACTUS), BioChaff::class.instance(), 1, 8),
        Entry("sugar_cane_to_bio_chaff", IngredientInput.item(Items.SUGAR_CANE), BioChaff::class.instance(), 1, 8),
        Entry("oak_leaves_to_bio_chaff", IngredientInput.item(Items.OAK_LEAVES), BioChaff::class.instance(), 1, 8),
        Entry("dead_bush_to_bio_chaff", IngredientInput.item(Items.DEAD_BUSH), BioChaff::class.instance(), 1, 8),
        Entry("weed_to_bio_chaff", IngredientInput.item(Weed::class.instance()), BioChaff::class.instance(), 1, 32),
        Entry("diamond_to_diamond_dust", IngredientInput.item(Items.DIAMOND), DiamondDust::class.instance(), 1),
        Entry("glowstone_to_glowstone_dust", IngredientInput.item(Items.GLOWSTONE), Items.GLOWSTONE_DUST, 4),
        Entry("quartz_block_to_quartz", IngredientInput.item(Items.QUARTZ_BLOCK), Items.QUARTZ, 4),
        // 金属锭 → 粉（用 tag 支持跨 mod 锭）
        Entry("copper_ingot_to_copper_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_COPPER, Items.COPPER_INGOT), CopperDust::class.instance(), 1),
        Entry("tin_ingot_to_tin_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_TIN, TinIngot::class.instance()), TinDust::class.instance(), 1),
        Entry("bronze_ingot_to_bronze_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_BRONZE, BronzeIngot::class.instance()), BronzeDust::class.instance(), 1),
        Entry("gold_ingot_to_gold_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_GOLD, Items.GOLD_INGOT), GoldDust::class.instance(), 1),
        Entry("iron_ingot_to_iron_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_IRON, Items.IRON_INGOT), IronDust::class.instance(), 1),
        Entry("lead_ingot_to_lead_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_LEAD, LeadIngot::class.instance()), LeadDust::class.instance(), 1),
        Entry("silver_ingot_to_silver_dust", IngredientInput.tag(ModTags.Compat.Items.INGOTS_SILVER, SilverIngot::class.instance()), SilverDust::class.instance(), 1),
        Entry("lapis_to_lapis_dust", IngredientInput.tag(ModTags.Compat.Items.GEMS_LAPIS, Items.LAPIS_LAZULI), LapisDust::class.instance(), 1),
        // 金属板 → 粉
        Entry("copper_plate_to_copper_dust", IngredientInput.tag(ModTags.Compat.Items.PLATES_COPPER, CopperPlate::class.instance()), CopperDust::class.instance(), 1),
        Entry("tin_plate_to_tin_dust", IngredientInput.tag(ModTags.Compat.Items.PLATES_TIN, TinPlate::class.instance()), TinDust::class.instance(), 1),
        Entry("bronze_plate_to_bronze_dust", IngredientInput.tag(ModTags.Compat.Items.PLATES_BRONZE, BronzePlate::class.instance()), BronzeDust::class.instance(), 1),
        Entry("gold_plate_to_gold_dust", IngredientInput.tag(ModTags.Compat.Items.PLATES_GOLD, GoldPlate::class.instance()), GoldDust::class.instance(), 1),
        Entry("iron_plate_to_iron_dust", IngredientInput.tag(ModTags.Compat.Items.PLATES_IRON, IronPlate::class.instance()), IronDust::class.instance(), 1),
        Entry("lead_plate_to_lead_dust", IngredientInput.tag(ModTags.Compat.Items.PLATES_LEAD, LeadPlate::class.instance()), LeadDust::class.instance(), 1),
        Entry("lapis_plate_to_lapis_dust", IngredientInput.tag(ModTags.Compat.Items.PLATES_LAPIS, LapisPlate::class.instance()), LapisDust::class.instance(), 1)
    )

    private val externalEntries = listOf(
        // Applied Energistics 2：两种水晶分别打成对应的 AE2 粉尘。
        ExternalEntry(
            "ae2_certus_quartz_crystal_to_dust",
            Identifier.fromNamespaceAndPath("ae2", "certus_quartz_crystal"),
            Identifier.fromNamespaceAndPath("ae2", "certus_quartz_dust")
        ),
        ExternalEntry(
            "ae2_fluix_crystal_to_dust",
            Identifier.fromNamespaceAndPath("ae2", "fluix_crystal"),
            Identifier.fromNamespaceAndPath("ae2", "fluix_dust")
        )
    )

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: RecipeOutput) {
        // TODO(1.21.1): RecipeOutput 不再接受自定义 RecipeJsonProvider，改为直接构造
        // MaceratorRecipe 实例交给 exporter（JSON 由 serializer codec 序列化）。
        entries.forEach { entry ->
            val id = Identifier.fromNamespaceAndPath("ic2_120", "macerating/${entry.name}")
            val recipe = MaceratorRecipe(
                id,
                entry.input.toIngredient(),
                ItemStack(entry.output, entry.count),
                entry.inputCount
            )
            exporter.accept(id, recipe, null)
        }
        externalEntries.forEach { entry ->
            val id = Identifier.fromNamespaceAndPath("ic2_120", "macerating/${entry.name}")
            val inputItem = BuiltInRegistries.ITEM.getValue(entry.input)
            val outputItem = BuiltInRegistries.ITEM.getValue(entry.output)
            if (inputItem == net.minecraft.world.item.Items.AIR || outputItem == net.minecraft.world.item.Items.AIR) return@forEach
            val recipe = MaceratorRecipe(
                id,
                Ingredient.of(inputItem),
                ItemStack(outputItem, entry.count),
                entry.inputCount
            )
            exporter.accept(id, recipe, null)
        }
    }
}

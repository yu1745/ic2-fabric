package ic2_120.content.recipes.macerator

import com.google.gson.JsonObject
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
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier
import java.util.function.Consumer

object MaceratorRecipeDatagen {
    data class Entry(
        val name: String,
        val input: IngredientInput,
        val output: Item,
        val count: Int,
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
        Entry("coal_to_coal_dust", IngredientInput.item(Items.COAL), CoalDust::class.instance(), 1),
        Entry("coal_block_to_coal_dust", IngredientInput.item(Items.COAL_BLOCK), CoalDust::class.instance(), 9),
        Entry("clay_block_to_clay_dust", IngredientInput.item(Items.CLAY), ClayDust::class.instance(), 2),
        Entry("wool_to_string", IngredientInput.tag(ItemTags.WOOL, Items.WHITE_WOOL), Items.STRING, 4),
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

    fun allEntries(): List<Entry> = entries

    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        entries.forEach { entry ->
            MaceratorRecipeJsonProvider(
                recipeId = Identifier("ic2_120", "macerating/${entry.name}"),
                input = entry.input,
                outputItem = entry.output,
                outputCount = entry.count,
                inputCount = entry.inputCount
            ).also(exporter::accept)
        }
    }

    private class MaceratorRecipeJsonProvider(
        private val recipeId: Identifier,
        private val input: IngredientInput,
        private val outputItem: Item,
        private val outputCount: Int,
        private val inputCount: Int = 1
    ) : RecipeJsonProvider {
        override fun serialize(json: JsonObject) {
            json.addProperty("type", "${ModMachineRecipes.recipeType(MaceratorRecipe::class)}")
            val ingredient = input.toJson()
            if (inputCount > 1) {
                ingredient.addProperty("count", inputCount)
            }
            json.add("ingredient", ingredient)

            val result = JsonObject()
            result.addProperty("item", Registries.ITEM.getId(outputItem).toString())
            result.addProperty("count", outputCount)
            json.add("result", result)
        }

        override fun getSerializer() = ModMachineRecipes.recipeSerializer(MaceratorRecipe::class)

        override fun getRecipeId(): Identifier = recipeId

        override fun toAdvancementJson(): JsonObject? = null

        override fun getAdvancementId(): Identifier? = null
    }
}

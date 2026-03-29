package ic2_120.content.item

import ic2_120.content.block.DeepslateLeadOreBlock
import ic2_120.content.block.DeepslateTinOreBlock
import ic2_120.content.block.DeepslateUraniumOreBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.content.recipes.ModTags
import ic2_120.registry.annotation.ModItem
import ic2_120.content.block.LeadBlock
import ic2_120.content.block.LeadOreBlock
import ic2_120.content.block.SilverBlock
import ic2_120.content.block.SteelBlock
import ic2_120.content.block.TinOreBlock
import ic2_120.content.block.UraniumOreBlock
import ic2_120.content.block.UraniumBlock
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

private fun offerIngotSmelting(
    exporter: Consumer<RecipeJsonProvider>,
    input: Item,
    output: Item,
    recipeId: Identifier,
    experience: Float = 0.1f,
    cookingTime: Int = 200
) {
    CookingRecipeJsonBuilder.createSmelting(
        Ingredient.ofItems(input),
        RecipeCategory.MISC,
        output,
        experience,
        cookingTime
    )
        .criterion(hasItem(input), conditionsFromItem(input))
        .offerTo(exporter, recipeId)
}

// ========== 锭（已存在：copper_ingot, tin_ingot, bronze_ingot 在 MetalItems.kt） ==========

/** 合金锭 */
@ModItem(name = "mixed_metal_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class MixedMetalIngot : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 配方1：铁板 + 青铜板 + 锡板 = 合金锭
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MixedMetalIngot::class.instance(), 2)
                .pattern("III")
                .pattern("BBB")
                .pattern("TTT")
                .input('I', Ingredient.fromTag(ModTags.Compat.Items.PLATES_IRON))
                .input('B', Ingredient.fromTag(ModTags.Compat.Items.PLATES_BRONZE))
                .input('T', Ingredient.fromTag(ModTags.Compat.Items.PLATES_TIN))
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, MixedMetalIngot::class.recipeId("from_plates"))

            // 配方2：钢锭 + 青铜锭 + 锡锭 = 合金锭
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MixedMetalIngot::class.instance(), 2)
                .pattern("III")
                .pattern("BBB")
                .pattern("TTT")
                .input('I', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_STEEL))
                .input('B', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE))
                .input('T', Ingredient.fromTag(ModTags.Compat.Items.INGOTS_TIN))
                .criterion(hasItem(SteelIngot::class.instance()), conditionsFromItem(SteelIngot::class.instance()))
                .offerTo(exporter, MixedMetalIngot::class.recipeId("from_ingots"))
        }
    }
}

/** 铅锭 */
@ModItem(name = "lead_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/lead"])
class LeadIngot : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, LeadIngot::class.instance(), 9)
                .input(LeadBlock::class.instance())
                .criterion(hasItem(LeadBlock::class.instance()), conditionsFromItem(LeadBlock::class.instance()))
                .offerTo(exporter, LeadIngot::class.recipeId("from_block"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LeadBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .input('I', LeadIngot::class.instance())
                .criterion(hasItem(LeadIngot::class.instance()), conditionsFromItem(LeadIngot::class.instance()))
                .offerTo(exporter, LeadBlock::class.recipeId("from_ingot"))

            offerIngotSmelting(
                exporter,
                LeadOreBlock::class.item(),
                LeadIngot::class.instance(),
                LeadIngot::class.recipeId("from_lead_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                DeepslateLeadOreBlock::class.item(),
                LeadIngot::class.instance(),
                LeadIngot::class.recipeId("from_deepslate_lead_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                CrushedLead::class.instance(),
                LeadIngot::class.instance(),
                LeadIngot::class.recipeId("from_crushed_lead_smelting")
            )
            offerIngotSmelting(
                exporter,
                PurifiedLead::class.instance(),
                LeadIngot::class.instance(),
                LeadIngot::class.recipeId("from_purified_lead_smelting")
            )
        }
    }
}

/** 银锭 */
@ModItem(name = "silver_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/silver"])
class SilverIngot : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SilverIngot::class.instance(), 9)
                .input(SilverBlock::class.instance())
                .criterion(hasItem(SilverBlock::class.instance()), conditionsFromItem(SilverBlock::class.instance()))
                .offerTo(exporter, SilverIngot::class.recipeId("from_block"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SilverBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .input('I', SilverIngot::class.instance())
                .criterion(hasItem(SilverIngot::class.instance()), conditionsFromItem(SilverIngot::class.instance()))
                .offerTo(exporter, SilverBlock::class.recipeId("from_ingot"))

            offerIngotSmelting(
                exporter,
                CrushedSilver::class.instance(),
                SilverIngot::class.instance(),
                SilverIngot::class.recipeId("from_crushed_silver_smelting")
            )
            offerIngotSmelting(
                exporter,
                PurifiedSilver::class.instance(),
                SilverIngot::class.instance(),
                SilverIngot::class.recipeId("from_purified_silver_smelting")
            )
        }
    }
}

/** 钢锭 */
@ModItem(name = "steel_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/steel"])
class SteelIngot : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SteelIngot::class.instance(), 9)
                .input(SteelBlock::class.instance())
                .criterion(hasItem(SteelBlock::class.instance()), conditionsFromItem(SteelBlock::class.instance()))
                .offerTo(exporter, SteelIngot::class.recipeId("from_block"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .input('I', SteelIngot::class.instance())
                .criterion(hasItem(SteelIngot::class.instance()), conditionsFromItem(SteelIngot::class.instance()))
                .offerTo(exporter, SteelBlock::class.recipeId("from_ingot"))
        }
    }
}

/** 精炼铁锭 */
@ModItem(name = "refined_iron_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/refined_iron"])
class RefinedIronIngot : Item(FabricItemSettings())

/** 铀锭 */
@ModItem(name = "uranium_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/uranium"])
class UraniumIngot : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, UraniumIngot::class.instance(), 9)
                .input(UraniumBlock::class.instance())
                .criterion(hasItem(UraniumBlock::class.instance()), conditionsFromItem(UraniumBlock::class.instance()))
                .offerTo(exporter, UraniumIngot::class.recipeId("from_block"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, UraniumBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .input('I', UraniumIngot::class.instance())
                .criterion(hasItem(UraniumIngot::class.instance()), conditionsFromItem(UraniumIngot::class.instance()))
                .offerTo(exporter, UraniumBlock::class.recipeId("from_ingot"))

            offerIngotSmelting(
                exporter,
                UraniumOreBlock::class.item(),
                UraniumIngot::class.instance(),
                UraniumIngot::class.recipeId("from_uranium_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                DeepslateUraniumOreBlock::class.item(),
                UraniumIngot::class.instance(),
                UraniumIngot::class.recipeId("from_deepslate_uranium_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                CrushedUranium::class.instance(),
                UraniumIngot::class.instance(),
                UraniumIngot::class.recipeId("from_crushed_uranium_smelting")
            )
            offerIngotSmelting(
                exporter,
                PurifiedUranium::class.instance(),
                UraniumIngot::class.instance(),
                UraniumIngot::class.recipeId("from_purified_uranium_smelting")
            )
        }
    }
}

// ========== 粗金属（raw，冶炼粗矿得） ==========

/** 粗铅 */
@ModItem(name = "raw_lead", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals", materialTags = ["raw_materials/lead"])
class RawLead : Item(FabricItemSettings())

/** 粗锡 */
@ModItem(name = "raw_tin", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals", materialTags = ["raw_materials/tin"])
class RawTin : Item(FabricItemSettings())

/** 粗铀 */
@ModItem(name = "raw_uranium", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals", materialTags = ["raw_materials/uranium"])
class RawUranium : Item(FabricItemSettings())

// ========== 粉碎矿石（打粉机产物） ==========

@ModItem(name = "crushed_copper", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedCopper : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            offerIngotSmelting(
                exporter,
                Items.COPPER_ORE,
                Items.COPPER_INGOT,
                CrushedCopper::class.recipeId("from_copper_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                Items.DEEPSLATE_COPPER_ORE,
                Items.COPPER_INGOT,
                CrushedCopper::class.recipeId("from_deepslate_copper_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                CrushedCopper::class.instance(),
                Items.COPPER_INGOT,
                CrushedCopper::class.recipeId("to_copper_ingot_smelting")
            )
            offerIngotSmelting(
                exporter,
                PurifiedCopper::class.instance(),
                Items.COPPER_INGOT,
                CrushedCopper::class.recipeId("from_purified_copper_smelting")
            )
        }
    }
}

@ModItem(name = "crushed_gold", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedGold : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            offerIngotSmelting(
                exporter,
                Items.GOLD_ORE,
                Items.GOLD_INGOT,
                CrushedGold::class.recipeId("from_gold_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                Items.DEEPSLATE_GOLD_ORE,
                Items.GOLD_INGOT,
                CrushedGold::class.recipeId("from_deepslate_gold_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                CrushedGold::class.instance(),
                Items.GOLD_INGOT,
                CrushedGold::class.recipeId("to_gold_ingot_smelting")
            )
            offerIngotSmelting(
                exporter,
                PurifiedGold::class.instance(),
                Items.GOLD_INGOT,
                CrushedGold::class.recipeId("from_purified_gold_smelting")
            )
        }
    }
}

@ModItem(name = "crushed_iron", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedIron : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            offerIngotSmelting(
                exporter,
                Items.IRON_ORE,
                Items.IRON_INGOT,
                CrushedIron::class.recipeId("from_iron_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                Items.DEEPSLATE_IRON_ORE,
                Items.IRON_INGOT,
                CrushedIron::class.recipeId("from_deepslate_iron_ore_smelting")
            )
            offerIngotSmelting(
                exporter,
                CrushedIron::class.instance(),
                Items.IRON_INGOT,
                CrushedIron::class.recipeId("to_iron_ingot_smelting")
            )
            offerIngotSmelting(
                exporter,
                PurifiedIron::class.instance(),
                Items.IRON_INGOT,
                CrushedIron::class.recipeId("from_purified_iron_smelting")
            )
        }
    }
}

@ModItem(name = "crushed_lead", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedLead : Item(FabricItemSettings())

@ModItem(name = "crushed_silver", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedSilver : Item(FabricItemSettings())

@ModItem(name = "crushed_tin", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedTin : Item(FabricItemSettings())

@ModItem(name = "crushed_uranium", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedUranium : Item(FabricItemSettings())

// ========== 纯净的粉碎矿石（洗矿机产物） ==========

@ModItem(name = "purified_copper", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedCopper : Item(FabricItemSettings())

@ModItem(name = "purified_gold", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedGold : Item(FabricItemSettings())

@ModItem(name = "purified_iron", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedIron : Item(FabricItemSettings())

@ModItem(name = "purified_lead", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedLead : Item(FabricItemSettings())

@ModItem(name = "purified_silver", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedSilver : Item(FabricItemSettings())

@ModItem(name = "purified_tin", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedTin : Item(FabricItemSettings())

@ModItem(name = "purified_uranium", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedUranium : Item(FabricItemSettings())

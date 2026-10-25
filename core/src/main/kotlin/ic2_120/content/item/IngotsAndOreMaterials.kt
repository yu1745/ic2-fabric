package ic2_120.content.item

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.has

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
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.Identifier
import ic2_120.registry.annotation.RecipeProvider

private fun offerIngotSmelting(
    exporter: RecipeOutput,
    input: Item,
    output: Item,
    recipeId: Identifier,
    experience: Float = 0.1f,
    cookingTime: Int = 200
) {
    SimpleCookingRecipeBuilder.smelting(
        Ingredient.of(input),
        RecipeCategory.MISC,
                net.minecraft.world.item.crafting.CookingBookCategory.MISC,
        output,
        experience,
        cookingTime
    )
        .unlockedBy(getHasName(input), has(input))
        .save(exporter, recipeId)
}

// ========== 锭（已存在：copper_ingot, tin_ingot, bronze_ingot 在 MetalItems.kt） ==========

/** 合金锭 */
@ModItem(name = "mixed_metal_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class MixedMetalIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 配方1：铁板 + 青铜板 + 锡板 = 合金锭
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, MixedMetalIngot::class.instance(), 2)
                .pattern("III")
                .pattern("BBB")
                .pattern("TTT")
                .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_IRON))
                .define('B', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_BRONZE))
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_TIN))
                .unlockedBy(getHasName(IronPlate::class.instance()), has(IronPlate::class.instance()))
                .save(exporter, MixedMetalIngot::class.recipeId("from_plates"))

            // 配方2：钢锭 + 青铜锭 + 锡锭 = 合金锭
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, MixedMetalIngot::class.instance(), 2)
                .pattern("III")
                .pattern("BBB")
                .pattern("TTT")
                .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_STEEL))
                .define('B', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_BRONZE))
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_TIN))
                .unlockedBy(getHasName(SteelIngot::class.instance()), has(SteelIngot::class.instance()))
                .save(exporter, MixedMetalIngot::class.recipeId("from_ingots"))
        }
    }
}

/** 铅锭 */
@ModItem(name = "lead_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/lead"])
class LeadIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, LeadIngot::class.instance(), 9)
                .requires(LeadBlock::class.instance())
                .unlockedBy(getHasName(LeadBlock::class.instance()), has(LeadBlock::class.instance()))
                .save(exporter, LeadIngot::class.recipeId("from_block"))

            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, LeadBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_LEAD))
                .unlockedBy(getHasName(LeadIngot::class.instance()), has(LeadIngot::class.instance()))
                .save(exporter, LeadBlock::class.recipeId("from_ingot"))

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
                RawLead::class.instance(),
                LeadIngot::class.instance(),
                LeadIngot::class.recipeId("from_raw_lead_smelting")
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
class SilverIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SilverIngot::class.instance(), 9)
                .requires(SilverBlock::class.instance())
                .unlockedBy(getHasName(SilverBlock::class.instance()), has(SilverBlock::class.instance()))
                .save(exporter, SilverIngot::class.recipeId("from_block"))

            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SilverBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_SILVER))
                .unlockedBy(getHasName(SilverIngot::class.instance()), has(SilverIngot::class.instance()))
                .save(exporter, SilverBlock::class.recipeId("from_ingot"))

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
class SteelIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SteelIngot::class.instance(), 9)
                .requires(SteelBlock::class.instance())
                .unlockedBy(getHasName(SteelBlock::class.instance()), has(SteelBlock::class.instance()))
                .save(exporter, SteelIngot::class.recipeId("from_block"))

            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SteelBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_STEEL))
                .unlockedBy(getHasName(SteelIngot::class.instance()), has(SteelIngot::class.instance()))
                .save(exporter, SteelBlock::class.recipeId("from_ingot"))
        }
    }
}

//已删除
/** 精炼铁锭 */
// @ModItem(name = "refined_iron_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/refined_iron"])
class RefinedIronIngot : Item(Item.Properties())

/** 铀锭 */
@ModItem(name = "uranium_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/uranium"])
class UraniumIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, UraniumIngot::class.instance(), 9)
                .requires(UraniumBlock::class.instance())
                .unlockedBy(getHasName(UraniumBlock::class.instance()), has(UraniumBlock::class.instance()))
                .save(exporter, UraniumIngot::class.recipeId("from_block"))

            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, UraniumBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.INGOTS_URANIUM))
                .unlockedBy(getHasName(UraniumIngot::class.instance()), has(UraniumIngot::class.instance()))
                .save(exporter, UraniumBlock::class.recipeId("from_ingot"))

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
                RawUranium::class.instance(),
                UraniumIngot::class.instance(),
                UraniumIngot::class.recipeId("from_raw_uranium_smelting")
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
class RawLead : Item(Item.Properties())

/** 粗锡 */
@ModItem(name = "raw_tin", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals", materialTags = ["raw_materials/tin"])
class RawTin : Item(Item.Properties())

/** 粗铀 */
@ModItem(name = "raw_uranium", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals", materialTags = ["raw_materials/uranium"])
class RawUranium : Item(Item.Properties())

// ========== 粉碎矿石（打粉机产物） ==========

@ModItem(name = "crushed_copper", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedCopper : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
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
class CrushedGold : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
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
class CrushedIron : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
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
class CrushedLead : Item(Item.Properties())

@ModItem(name = "crushed_silver", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedSilver : Item(Item.Properties())

@ModItem(name = "crushed_tin", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedTin : Item(Item.Properties())

@ModItem(name = "crushed_uranium", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedUranium : Item(Item.Properties())

// ========== 纯净的粉碎矿石（洗矿机产物） ==========

@ModItem(name = "purified_copper", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedCopper : Item(Item.Properties())

@ModItem(name = "purified_gold", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedGold : Item(Item.Properties())

@ModItem(name = "purified_iron", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedIron : Item(Item.Properties())

@ModItem(name = "purified_lead", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedLead : Item(Item.Properties())

@ModItem(name = "purified_silver", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedSilver : Item(Item.Properties())

@ModItem(name = "purified_tin", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedTin : Item(Item.Properties())

@ModItem(name = "purified_uranium", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedUranium : Item(Item.Properties())

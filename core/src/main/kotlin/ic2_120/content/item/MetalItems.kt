package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName
import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.annotation.ModItem
import ic2_120.content.block.BronzeBlock
import ic2_120.content.block.DeepslateTinOreBlock
import ic2_120.content.block.TinBlock
import ic2_120.content.block.TinOreBlock
import ic2_120.content.item.RawTin
import ic2_120.content.recipes.ModTags
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import ic2_120.registry.annotation.RecipeProvider

// 铜锭：使用原版 minecraft:copper_ingot，此处不再注册

/**
 * 锡锭。
 */
@ModItem(name = "tin_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/tin"])
class TinIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TinIngot::class.instance(), 9)
                .requires(TinBlock::class.instance())
                .unlockedBy(getHasName(TinBlock::class.instance()), has(TinBlock::class.instance()))
                .save(exporter, TinIngot::class.recipeId("from_block"))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_TIN))
                .unlockedBy(getHasName(TinIngot::class.instance()), has(TinIngot::class.instance()))
                .save(exporter, TinBlock::class.recipeId("from_ingot"))

            SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(TinOreBlock::class.item()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(getHasName(TinOreBlock::class.item()), has(TinOreBlock::class.item()))
                .save(exporter, TinIngot::class.recipeId("from_tin_ore_smelting"))

            SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(DeepslateTinOreBlock::class.item()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(
                    getHasName(DeepslateTinOreBlock::class.item()),
                    has(DeepslateTinOreBlock::class.item())
                )
                .save(exporter, TinIngot::class.recipeId("from_deepslate_tin_ore_smelting"))

            SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(RawTin::class.instance()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(getHasName(RawTin::class.instance()), has(RawTin::class.instance()))
                .save(exporter, TinIngot::class.recipeId("from_raw_tin_smelting"))

            SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(CrushedTin::class.instance()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(getHasName(CrushedTin::class.instance()), has(CrushedTin::class.instance()))
                .save(exporter, TinIngot::class.recipeId("from_crushed_tin_smelting"))

            SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(PurifiedTin::class.instance()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(getHasName(PurifiedTin::class.instance()), has(PurifiedTin::class.instance()))
                .save(exporter, TinIngot::class.recipeId("from_purified_tin_smelting"))
        }
    }
}

/**
 * 青铜锭。
 */
@ModItem(name = "bronze_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/bronze"])
class BronzeIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, BronzeIngot::class.instance(), 9)
                .requires(BronzeBlock::class.instance())
                .unlockedBy(getHasName(BronzeBlock::class.instance()), has(BronzeBlock::class.instance()))
                .save(exporter, BronzeIngot::class.recipeId("from_block"))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzeBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE))
                .unlockedBy(getHasName(BronzeIngot::class.instance()), has(BronzeIngot::class.instance()))
                .save(exporter, BronzeBlock::class.recipeId("from_ingot"))
        }
    }
}

/**
 * 橡胶。
 */
@ModItem(name = "rubber", tab = CreativeTab.IC2_MATERIALS, group = "materials", materialTags = ["rubber"])
class RubberItem : Item(Item.Properties())


package ic2_120.content.item

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
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import ic2_120.registry.annotation.RecipeProvider

// 铜锭：使用原版 minecraft:copper_ingot，此处不再注册

/**
 * 锡锭。
 */
@ModItem(name = "tin_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/tin"])
class TinIngot : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TinIngot::class.instance(), 9)
                .input(TinBlock::class.instance())
                .criterion(hasItem(TinBlock::class.instance()), conditionsFromItem(TinBlock::class.instance()))
                .offerTo(exporter, TinIngot::class.recipeId("from_block"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, TinBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .input('I', TinIngot::class.instance())
                .criterion(hasItem(TinIngot::class.instance()), conditionsFromItem(TinIngot::class.instance()))
                .offerTo(exporter, TinBlock::class.recipeId("from_ingot"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(TinOreBlock::class.item()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(TinOreBlock::class.item()), conditionsFromItem(TinOreBlock::class.item()))
                .offerTo(exporter, TinIngot::class.recipeId("from_tin_ore_smelting"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(DeepslateTinOreBlock::class.item()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(
                    hasItem(DeepslateTinOreBlock::class.item()),
                    conditionsFromItem(DeepslateTinOreBlock::class.item())
                )
                .offerTo(exporter, TinIngot::class.recipeId("from_deepslate_tin_ore_smelting"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(RawTin::class.instance()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(RawTin::class.instance()), conditionsFromItem(RawTin::class.instance()))
                .offerTo(exporter, TinIngot::class.recipeId("from_raw_tin_smelting"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(CrushedTin::class.instance()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(CrushedTin::class.instance()), conditionsFromItem(CrushedTin::class.instance()))
                .offerTo(exporter, TinIngot::class.recipeId("from_crushed_tin_smelting"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(PurifiedTin::class.instance()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(PurifiedTin::class.instance()), conditionsFromItem(PurifiedTin::class.instance()))
                .offerTo(exporter, TinIngot::class.recipeId("from_purified_tin_smelting"))
        }
    }
}

/**
 * 青铜锭。
 */
@ModItem(name = "bronze_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots", materialTags = ["ingots/bronze"])
class BronzeIngot : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeIngot::class.instance(), 9)
                .input(BronzeBlock::class.instance())
                .criterion(hasItem(BronzeBlock::class.instance()), conditionsFromItem(BronzeBlock::class.instance()))
                .offerTo(exporter, BronzeIngot::class.recipeId("from_block"))

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeBlock::class.instance(), 1)
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .input('I', BronzeIngot::class.instance())
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzeBlock::class.recipeId("from_ingot"))
        }
    }
}

/**
 * 橡胶。
 */
@ModItem(name = "rubber", tab = CreativeTab.IC2_MATERIALS, group = "materials", materialTags = ["rubber"])
class RubberItem : Item(Item.Settings())


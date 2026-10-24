package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory

// ========== 植物种子类 ==========

@ModItem(name = "fertilizer", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Fertilizer : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 两个废料 + 一个骨粉 -> 两个肥料
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Fertilizer::class.instance(), 2)
                .requires(Scrap::class.instance(), 2)
                .requires(Items.BONE_MEAL)
                .unlockedBy(getHasName(Scrap::class.instance()), has(Scrap::class.instance()))
                .save(exporter, Fertilizer::class.recipeId("from_scrap_and_bone_meal"))

            // 一个肥料 + 两个废料 -> 两个肥料
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Fertilizer::class.instance(), 2)
                .requires(Fertilizer::class.instance())
                .requires(Scrap::class.instance(), 2)
                .unlockedBy(getHasName(Fertilizer::class.instance()), has(Fertilizer::class.instance()))
                .save(exporter, Fertilizer::class.recipeId("from_fertilizer_and_scrap"))
        }
    }
}

@ModItem(name = "grin_powder", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class GrinPowder : Item(Item.Properties())

@ModItem(name = "hops", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Hops : Item(Item.Properties())

@ModItem(name = "weed", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Weed : Item(Item.Properties())

@ModItem(name = "terra_wart", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class TerraWart : Item(Item.Properties())

@ModItem(name = "coffee_beans", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeeBeans : Item(Item.Properties())

@ModItem(name = "coffee_powder", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeePowder : Item(Item.Properties())

/** 面粉（小麦提取） */
@ModItem(name = "flour", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Flour : Item(Item.Properties())

// ========== 饮料杯类 ==========

@ModItem(name = "empty_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class EmptyMug : Item(Item.Properties())

@ModItem(name = "coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class CoffeeMug : Item(Item.Properties())

@ModItem(name = "cold_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class ColdCoffeeMug : Item(Item.Properties())

@ModItem(name = "dark_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class DarkCoffeeMug : Item(Item.Properties())

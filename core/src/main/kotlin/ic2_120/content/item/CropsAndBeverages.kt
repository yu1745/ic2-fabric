package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

// ========== 植物种子类 ==========

@ModItem(name = "fertilizer", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Fertilizer : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 两个废料 + 一个骨粉 -> 两个肥料
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Fertilizer::class.instance(), 2)
                .input(Scrap::class.instance(), 2)
                .input(Items.BONE_MEAL)
                .criterion(hasItem(Scrap::class.instance()), conditionsFromItem(Scrap::class.instance()))
                .offerTo(exporter, Fertilizer::class.recipeId("from_scrap_and_bone_meal"))

            // 一个肥料 + 两个废料 -> 两个肥料
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Fertilizer::class.instance(), 2)
                .input(Fertilizer::class.instance())
                .input(Scrap::class.instance(), 2)
                .criterion(hasItem(Fertilizer::class.instance()), conditionsFromItem(Fertilizer::class.instance()))
                .offerTo(exporter, Fertilizer::class.recipeId("from_fertilizer_and_scrap"))
        }
    }
}

@ModItem(name = "grin_powder", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class GrinPowder : Item(FabricItemSettings())

@ModItem(name = "hops", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Hops : Item(FabricItemSettings())

@ModItem(name = "weed", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Weed : Item(FabricItemSettings())

@ModItem(name = "terra_wart", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class TerraWart : Item(FabricItemSettings())

@ModItem(name = "coffee_beans", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeeBeans : Item(FabricItemSettings())

@ModItem(name = "coffee_powder", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeePowder : Item(FabricItemSettings())

/** 面粉（小麦提取） */
@ModItem(name = "flour", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Flour : Item(FabricItemSettings())

// ========== 饮料杯类 ==========

@ModItem(name = "empty_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class EmptyMug : Item(FabricItemSettings())

@ModItem(name = "coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class CoffeeMug : Item(FabricItemSettings())

@ModItem(name = "cold_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class ColdCoffeeMug : Item(FabricItemSettings())

@ModItem(name = "dark_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class DarkCoffeeMug : Item(FabricItemSettings())

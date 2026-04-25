package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

// ========== 植物种子类 ==========

@ModItem(name = "fertilizer", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Fertilizer : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
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
class GrinPowder : Item(Item.Settings())

@ModItem(name = "hops", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Hops : Item(Item.Settings())

@ModItem(name = "weed", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Weed : Item(Item.Settings())

@ModItem(name = "terra_wart", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class TerraWart : Item(Item.Settings())

@ModItem(name = "coffee_beans", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeeBeans : Item(Item.Settings())

@ModItem(name = "coffee_powder", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeePowder : Item(Item.Settings())

/** 面粉（小麦提取） */
@ModItem(name = "flour", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Flour : Item(Item.Settings())

// ========== 饮料杯类 ==========

@ModItem(name = "empty_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class EmptyMug : Item(Item.Settings())

@ModItem(name = "coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class CoffeeMug : Item(Item.Settings())

@ModItem(name = "cold_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class ColdCoffeeMug : Item(Item.Settings())

@ModItem(name = "dark_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class DarkCoffeeMug : Item(Item.Settings())

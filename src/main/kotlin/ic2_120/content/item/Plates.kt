package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.content.recipes.ModTags
import ic2_120.content.recipes.crafting.DamageToolShapelessRecipeDatagen
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import java.util.function.Consumer
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.annotation.RecipeProvider

// ========== 板类（金属成型机切割：1 锭 -> 1 板；青金石/黑曜石见配方） ==========

@ModItem(name = "bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/bronze"])
class BronzePlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = BronzePlate::class.id(),
                result = BronzePlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.INGOTS_BRONZE)
                )
            )
        }
    }
}

@ModItem(name = "copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/copper"])
class CopperPlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = CopperPlate::class.id(),
                result = CopperPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    Ingredient.fromTag(ModTags.Compat.Items.INGOTS_COPPER),
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance())
                )
            )
        }
    }
}

@ModItem(name = "gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/gold"])
class GoldPlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = GoldPlate::class.id(),
                result = GoldPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    Ingredient.fromTag(ModTags.Compat.Items.INGOTS_GOLD),
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance())
                )
            )
        }
    }
}

@ModItem(name = "iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/iron"])
class IronPlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = IronPlate::class.id(),
                result = IronPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.INGOTS_IRON)
                )
            )
        }
    }
}

@ModItem(name = "lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/lapis"])
class LapisPlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LapisPlate::class.id(),
                result = LapisPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.GEMS_LAPIS)
                )
            )
        }
    }
}

@ModItem(name = "lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/lead"])
class LeadPlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LeadPlate::class.id(),
                result = LeadPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.INGOTS_LEAD)
                )
            )
        }
    }
}

@ModItem(name = "obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/obsidian"])
class ObsidianPlate : Item(Item.Settings())

@ModItem(name = "steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/steel"])
class SteelPlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = SteelPlate::class.id(),
                result = SteelPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.INGOTS_STEEL)
                )
            )
        }
    }
}

@ModItem(name = "tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/tin"])
class TinPlate : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TinPlate::class.id(),
                result = TinPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.INGOTS_TIN)
                )
            )
        }
    }
}

// ========== 致密板类（压缩机：9 板 -> 1 致密板） ==========

@ModItem(name = "dense_bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseBronzePlate : Item(Item.Settings())

@ModItem(name = "dense_copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseCopperPlate : Item(Item.Settings())

@ModItem(name = "dense_gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseGoldPlate : Item(Item.Settings())

@ModItem(name = "dense_iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseIronPlate : Item(Item.Settings())

@ModItem(name = "dense_lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLapisPlate : Item(Item.Settings())

@ModItem(name = "dense_lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLeadPlate : Item(Item.Settings())

@ModItem(name = "dense_obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseObsidianPlate : Item(Item.Settings())

@ModItem(name = "dense_steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseSteelPlate : Item(Item.Settings())

@ModItem(name = "dense_tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseTinPlate : Item(Item.Settings())

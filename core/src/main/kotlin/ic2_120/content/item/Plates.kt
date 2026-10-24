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
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import ic2_120.registry.annotation.RecipeProvider

// ========== 板类（金属成型机切割：1 锭 -> 1 板；青金石/黑曜石见配方） ==========

@ModItem(name = "bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/bronze"])
class BronzePlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = BronzePlate::class.id(),
                result = BronzePlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.INGOTS_BRONZE)
                )
            )
        }
    }
}

@ModItem(name = "copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/copper"])
class CopperPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = CopperPlate::class.id(),
                result = CopperPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    Ingredient.of(ModTags.Compat.Items.INGOTS_COPPER),
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance())
                )
            )
        }
    }
}

@ModItem(name = "gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/gold"])
class GoldPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = GoldPlate::class.id(),
                result = GoldPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    Ingredient.of(ModTags.Compat.Items.INGOTS_GOLD),
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance())
                )
            )
        }
    }
}

@ModItem(name = "iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/iron"])
class IronPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = IronPlate::class.id(),
                result = IronPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.INGOTS_IRON)
                )
            )
        }
    }
}

@ModItem(name = "lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/lapis"])
class LapisPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LapisPlate::class.id(),
                result = LapisPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.GEMS_LAPIS)
                )
            )
        }
    }
}

@ModItem(name = "lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/lead"])
class LeadPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LeadPlate::class.id(),
                result = LeadPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.INGOTS_LEAD)
                )
            )
        }
    }
}

@ModItem(name = "obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/obsidian"])
class ObsidianPlate : Item(Item.Properties())

@ModItem(name = "steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/steel"])
class SteelPlate : Item(Item.Properties())

@ModItem(name = "tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates", materialTags = ["plates/tin"])
class TinPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TinPlate::class.id(),
                result = TinPlate::class.instance(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.INGOTS_TIN)
                )
            )
        }
    }
}

// ========== 致密板类（压缩机：9 板 -> 1 致密板） ==========

@ModItem(name = "dense_bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseBronzePlate : Item(Item.Properties())

@ModItem(name = "dense_copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseCopperPlate : Item(Item.Properties())

@ModItem(name = "dense_gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseGoldPlate : Item(Item.Properties())

@ModItem(name = "dense_iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseIronPlate : Item(Item.Properties())

@ModItem(name = "dense_lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLapisPlate : Item(Item.Properties())

@ModItem(name = "dense_lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLeadPlate : Item(Item.Properties())

@ModItem(name = "dense_obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseObsidianPlate : Item(Item.Properties())

@ModItem(name = "dense_steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseSteelPlate : Item(Item.Properties())

@ModItem(name = "dense_tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseTinPlate : Item(Item.Properties())

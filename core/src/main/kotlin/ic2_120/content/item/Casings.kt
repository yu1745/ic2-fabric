package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.content.recipes.crafting.DamageToolShapelessRecipeDatagen
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.item.Item
import net.minecraft.recipe.Ingredient
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.annotation.RecipeProvider

// ========== 外壳类 ==========

/** 青铜外壳 */
@ModItem(name = "bronze_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class BronzeCasing : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = BronzeCasing::class.id(),
                result = BronzeCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.ofItems(BronzePlate::class.instance())
                )
            )
        }
    }
}

/** 铜质外壳 */
@ModItem(name = "copper_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class CopperCasing : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = CopperCasing::class.id(),
                result = CopperCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.ofItems(CopperPlate::class.instance())
                )
            )
        }
    }
}

/** 黄金外壳 */
@ModItem(name = "gold_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class GoldCasing : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = GoldCasing::class.id(),
                result = GoldCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.ofItems(GoldPlate::class.instance())
                )
            )
        }
    }
}

/** 铁质外壳 */
@ModItem(name = "iron_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class IronCasing : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = IronCasing::class.id(),
                result = IronCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.ofItems(IronPlate::class.instance())
                )
            )
        }
    }
}

/** 铅质外壳 */
@ModItem(name = "lead_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class LeadCasing : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LeadCasing::class.id(),
                result = LeadCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.ofItems(LeadPlate::class.instance())
                )
            )
        }
    }
}

/** 钢质外壳 */
@ModItem(name = "steel_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class SteelCasing : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = SteelCasing::class.id(),
                result = SteelCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.ofItems(SteelPlate::class.instance())
                )
            )
        }
    }
}

/** 锡质外壳 */
@ModItem(name = "tin_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class TinCasing : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TinCasing::class.id(),
                result = TinCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.ofItems(TinPlate::class.instance())
                )
            )
        }
    }
}

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
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import ic2_120.registry.annotation.RecipeProvider

// ========== 外壳类 ==========

/** 青铜外壳 */
@ModItem(name = "bronze_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class BronzeCasing : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = BronzeCasing::class.id(),
                result = BronzeCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE)
                )
            )
        }
    }
}

/** 铜质外壳 */
@ModItem(name = "copper_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class CopperCasing : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = CopperCasing::class.id(),
                result = CopperCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_COPPER)
                )
            )
        }
    }
}

/** 黄金外壳 */
@ModItem(name = "gold_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class GoldCasing : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = GoldCasing::class.id(),
                result = GoldCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_GOLD)
                )
            )
        }
    }
}

/** 铁质外壳 */
@ModItem(name = "iron_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class IronCasing : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = IronCasing::class.id(),
                result = IronCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_IRON)
                )
            )
        }
    }
}

/** 铅质外壳 */
@ModItem(name = "lead_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class LeadCasing : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = LeadCasing::class.id(),
                result = LeadCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_LEAD)
                )
            )
        }
    }
}

/** 钢质外壳 */
@ModItem(name = "steel_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class SteelCasing : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = SteelCasing::class.id(),
                result = SteelCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_STEEL)
                )
            )
        }
    }
}

/** 锡质外壳 */
@ModItem(name = "tin_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class TinCasing : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TinCasing::class.id(),
                result = TinCasing::class.instance(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(ForgeHammer::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_TIN)
                )
            )
        }
    }
}

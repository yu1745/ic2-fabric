package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName
import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import net.minecraft.world.item.Item
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import ic2_120.registry.annotation.RecipeProvider

// ========== 核能相关材料 ==========

/** 浓缩铀核燃料 */
@ModItem(name = "uranium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Uranium::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .define('X', Ingredient.of(ModTags.Compat.Items.INGOTS_URANIUM))
                .define('O', SmallUranium235::class.instance())
                .unlockedBy(getHasName(UraniumIngot::class.instance()), has(UraniumIngot::class.instance()))
                .save(exporter, Uranium::class.recipeId("1"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Uranium::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .define('X', Uranium238::class.instance())
                .define('O', SmallUranium235::class.instance())
                .unlockedBy(getHasName(Uranium238::class.instance()), has(Uranium238::class.instance()))
                .save(exporter, Uranium::class.recipeId("2"))
        }
    }
}

/** 铀 -235 */
@ModItem(name = "uranium_235", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium235 : Item(Item.Properties())

/** 铀 -238 */
@ModItem(name = "uranium_238", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium238 : Item(Item.Properties())

/** 钚 */
@ModItem(name = "plutonium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Plutonium : Item(Item.Properties())

/** 钚铀混合氧化物核燃料 (MOX) */
@ModItem(name = "mox", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Mox : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Mox::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .define('X', Ingredient.of(ModTags.Compat.Items.INGOTS_URANIUM))
                .define('O', Plutonium::class.instance())
                .unlockedBy(getHasName(UraniumIngot::class.instance()), has(UraniumIngot::class.instance()))
                .save(exporter, Mox::class.recipeId("1"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Mox::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .define('X', Uranium238::class.instance())
                .define('O', Plutonium::class.instance())
                .unlockedBy(getHasName(Uranium238::class.instance()), has(Uranium238::class.instance()))
                .save(exporter, Mox::class.recipeId("2"))
        }
    }
}

/** 小撮铀 -235 */
@ModItem(name = "small_uranium_235", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallUranium235 : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 9 小撮 -> 1 铀-235
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Uranium235::class.instance(), 1)
                .requires(SmallUranium235::class.instance()).requires(SmallUranium235::class.instance())
                .requires(SmallUranium235::class.instance()).requires(SmallUranium235::class.instance())
                .requires(SmallUranium235::class.instance()).requires(SmallUranium235::class.instance())
                .requires(SmallUranium235::class.instance()).requires(SmallUranium235::class.instance())
                .requires(SmallUranium235::class.instance()).unlockedBy(
                    getHasName(SmallUranium235::class.instance()), has(SmallUranium235::class.instance())
                ).save(exporter, ResourceLocation.fromNamespaceAndPath("ic2_120", "small_uranium_235_to_uranium_235"))
            // 1 铀-235 -> 9 小撮
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallUranium235::class.instance(), 9)
                .requires(Uranium235::class.instance())
                .unlockedBy(getHasName(Uranium235::class.instance()), has(Uranium235::class.instance()))
                .save(exporter, ResourceLocation.fromNamespaceAndPath("ic2_120", "uranium_235_to_small_uranium_235"))
        }
    }
}

/** 小撮铀 -238 */
@ModItem(name = "small_uranium_238", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallUranium238 : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 9 小撮 -> 1 铀-238
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Uranium238::class.instance(), 1)
                .requires(SmallUranium238::class.instance()).requires(SmallUranium238::class.instance())
                .requires(SmallUranium238::class.instance()).requires(SmallUranium238::class.instance())
                .requires(SmallUranium238::class.instance()).requires(SmallUranium238::class.instance())
                .requires(SmallUranium238::class.instance()).requires(SmallUranium238::class.instance())
                .requires(SmallUranium238::class.instance()).unlockedBy(
                    getHasName(SmallUranium238::class.instance()), has(SmallUranium238::class.instance())
                ).save(exporter, ResourceLocation.fromNamespaceAndPath("ic2_120", "small_uranium_238_to_uranium_238"))
            // 1 铀-238 -> 9 小撮
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallUranium238::class.instance(), 9)
                .requires(Uranium238::class.instance())
                .unlockedBy(getHasName(Uranium238::class.instance()), has(Uranium238::class.instance()))
                .save(exporter, ResourceLocation.fromNamespaceAndPath("ic2_120", "uranium_238_to_small_uranium_238"))
        }
    }
}

/** 小撮钚 */
@ModItem(name = "small_plutonium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallPlutonium : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 9 小撮 -> 1 钚
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Plutonium::class.instance(), 1)
                .requires(SmallPlutonium::class.instance()).requires(SmallPlutonium::class.instance())
                .requires(SmallPlutonium::class.instance()).requires(SmallPlutonium::class.instance())
                .requires(SmallPlutonium::class.instance()).requires(SmallPlutonium::class.instance())
                .requires(SmallPlutonium::class.instance()).requires(SmallPlutonium::class.instance())
                .requires(SmallPlutonium::class.instance()).unlockedBy(
                    getHasName(SmallPlutonium::class.instance()), has(SmallPlutonium::class.instance())
                ).save(exporter, ResourceLocation.fromNamespaceAndPath("ic2_120", "small_plutonium_to_plutonium"))
            // 1 钚 -> 9 小撮
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallPlutonium::class.instance(), 9)
                .requires(Plutonium::class.instance())
                .unlockedBy(getHasName(Plutonium::class.instance()), has(Plutonium::class.instance()))
                .save(exporter, ResourceLocation.fromNamespaceAndPath("ic2_120", "plutonium_to_small_plutonium"))
        }
    }
}

/** 浓缩铀核燃料靶丸 */
//@Deprecated
//@ModItem(name = "uranium_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
//class UraniumPellet : Item(Item.Properties())

///** 钚铀混合氧化物核燃料靶丸 (MOX) */
//@Deprecated
//@ModItem(name = "mox_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
//class MoxPellet : Item(Item.Properties())

/** 放射性同位素燃料靶丸 */
@ModItem(name = "rtg_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class RtgPellet : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RtgPellet::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .define('X', DenseIronPlate::class.instance())
                .define('O', Plutonium::class.instance())
                .unlockedBy(getHasName(DenseIronPlate::class.instance()), has(DenseIronPlate::class.instance()))
                .save(exporter, RtgPellet::class.id())
        }
    }
}


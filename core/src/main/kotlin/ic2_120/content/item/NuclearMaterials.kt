package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import net.minecraft.item.Item
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import ic2_120.registry.annotation.RecipeProvider

// ========== 核能相关材料 ==========

/** 浓缩铀核燃料 */
@ModItem(name = "uranium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Uranium::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .input('X', UraniumIngot::class.instance())
                .input('O', SmallUranium235::class.instance())
                .criterion(hasItem(UraniumIngot::class.instance()), conditionsFromItem(UraniumIngot::class.instance()))
                .offerTo(exporter, Uranium::class.recipeId("1"))
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Uranium::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .input('X', Uranium238::class.instance())
                .input('O', SmallUranium235::class.instance())
                .criterion(hasItem(Uranium238::class.instance()), conditionsFromItem(Uranium238::class.instance()))
                .offerTo(exporter, Uranium::class.recipeId("2"))
        }
    }
}

/** 铀 -235 */
@ModItem(name = "uranium_235", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium235 : Item(Item.Settings())

/** 铀 -238 */
@ModItem(name = "uranium_238", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Uranium238 : Item(Item.Settings())

/** 钚 */
@ModItem(name = "plutonium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Plutonium : Item(Item.Settings())

/** 钚铀混合氧化物核燃料 (MOX) */
@ModItem(name = "mox", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class Mox : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Mox::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .input('X', UraniumIngot::class.instance())
                .input('O', Plutonium::class.instance())
                .criterion(hasItem(UraniumIngot::class.instance()), conditionsFromItem(UraniumIngot::class.instance()))
                .offerTo(exporter, Mox::class.recipeId("1"))
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Mox::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .input('X', Uranium238::class.instance())
                .input('O', Plutonium::class.instance())
                .criterion(hasItem(Uranium238::class.instance()), conditionsFromItem(Uranium238::class.instance()))
                .offerTo(exporter, Mox::class.recipeId("2"))
        }
    }
}

/** 小撮铀 -235 */
@ModItem(name = "small_uranium_235", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallUranium235 : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 9 小撮 -> 1 铀-235
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Uranium235::class.instance(), 1)
                .input(SmallUranium235::class.instance()).input(SmallUranium235::class.instance())
                .input(SmallUranium235::class.instance()).input(SmallUranium235::class.instance())
                .input(SmallUranium235::class.instance()).input(SmallUranium235::class.instance())
                .input(SmallUranium235::class.instance()).input(SmallUranium235::class.instance())
                .input(SmallUranium235::class.instance()).criterion(
                    hasItem(SmallUranium235::class.instance()), conditionsFromItem(SmallUranium235::class.instance())
                ).offerTo(exporter, Identifier.of("ic2_120", "small_uranium_235_to_uranium_235"))
            // 1 铀-235 -> 9 小撮
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallUranium235::class.instance(), 9)
                .input(Uranium235::class.instance())
                .criterion(hasItem(Uranium235::class.instance()), conditionsFromItem(Uranium235::class.instance()))
                .offerTo(exporter, Identifier.of("ic2_120", "uranium_235_to_small_uranium_235"))
        }
    }
}

/** 小撮铀 -238 */
@ModItem(name = "small_uranium_238", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallUranium238 : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 9 小撮 -> 1 铀-238
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Uranium238::class.instance(), 1)
                .input(SmallUranium238::class.instance()).input(SmallUranium238::class.instance())
                .input(SmallUranium238::class.instance()).input(SmallUranium238::class.instance())
                .input(SmallUranium238::class.instance()).input(SmallUranium238::class.instance())
                .input(SmallUranium238::class.instance()).input(SmallUranium238::class.instance())
                .input(SmallUranium238::class.instance()).criterion(
                    hasItem(SmallUranium238::class.instance()), conditionsFromItem(SmallUranium238::class.instance())
                ).offerTo(exporter, Identifier.of("ic2_120", "small_uranium_238_to_uranium_238"))
            // 1 铀-238 -> 9 小撮
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallUranium238::class.instance(), 9)
                .input(Uranium238::class.instance())
                .criterion(hasItem(Uranium238::class.instance()), conditionsFromItem(Uranium238::class.instance()))
                .offerTo(exporter, Identifier.of("ic2_120", "uranium_238_to_small_uranium_238"))
        }
    }
}

/** 小撮钚 */
@ModItem(name = "small_plutonium", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class SmallPlutonium : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 9 小撮 -> 1 钚
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Plutonium::class.instance(), 1)
                .input(SmallPlutonium::class.instance()).input(SmallPlutonium::class.instance())
                .input(SmallPlutonium::class.instance()).input(SmallPlutonium::class.instance())
                .input(SmallPlutonium::class.instance()).input(SmallPlutonium::class.instance())
                .input(SmallPlutonium::class.instance()).input(SmallPlutonium::class.instance())
                .input(SmallPlutonium::class.instance()).criterion(
                    hasItem(SmallPlutonium::class.instance()), conditionsFromItem(SmallPlutonium::class.instance())
                ).offerTo(exporter, Identifier.of("ic2_120", "small_plutonium_to_plutonium"))
            // 1 钚 -> 9 小撮
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallPlutonium::class.instance(), 9)
                .input(Plutonium::class.instance())
                .criterion(hasItem(Plutonium::class.instance()), conditionsFromItem(Plutonium::class.instance()))
                .offerTo(exporter, Identifier.of("ic2_120", "plutonium_to_small_plutonium"))
        }
    }
}

/** 浓缩铀核燃料靶丸 */
//@Deprecated
//@ModItem(name = "uranium_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
//class UraniumPellet : Item(Item.Settings())

///** 钚铀混合氧化物核燃料靶丸 (MOX) */
//@Deprecated
//@ModItem(name = "mox_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
//class MoxPellet : Item(Item.Settings())

/** 放射性同位素燃料靶丸 */
@ModItem(name = "rtg_pellet", tab = CreativeTab.IC2_MATERIALS, group = "nuclear")
class RtgPellet : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RtgPellet::class.instance(), 1)
                .pattern("XXX")
                .pattern("OOO")
                .pattern("XXX")
                .input('X', DenseIronPlate::class.instance())
                .input('O', Plutonium::class.instance())
                .criterion(hasItem(DenseIronPlate::class.instance()), conditionsFromItem(DenseIronPlate::class.instance()))
                .offerTo(exporter, RtgPellet::class.id())
        }
    }
}


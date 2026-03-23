package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import java.util.function.Consumer
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem

// ========== 外壳类 ==========

/** 青铜外壳 */
@ModItem(name = "bronze_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class BronzeCasing : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeCasing::class.instance(), 2)
                .input(ForgeHammer::class.instance())
                .input(BronzePlate::class.instance())
                .criterion(hasItem(BronzePlate::class.instance()), conditionsFromItem(BronzePlate::class.instance()))
                .offerTo(exporter, BronzeCasing::class.id())
        }
    }
}

/** 铜质外壳 */
@ModItem(name = "copper_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class CopperCasing : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, CopperCasing::class.instance(), 2)
                .input(ForgeHammer::class.instance())
                .input(CopperPlate::class.instance())
                .criterion(hasItem(CopperPlate::class.instance()), conditionsFromItem(CopperPlate::class.instance()))
                .offerTo(exporter, CopperCasing::class.id())
        }
    }
}

/** 黄金外壳 */
@ModItem(name = "gold_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class GoldCasing : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, GoldCasing::class.instance(), 2)
                .input(ForgeHammer::class.instance())
                .input(GoldPlate::class.instance())
                .criterion(hasItem(GoldPlate::class.instance()), conditionsFromItem(GoldPlate::class.instance()))
                .offerTo(exporter, GoldCasing::class.id())
        }
    }
}

/** 铁质外壳 */
@ModItem(name = "iron_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class IronCasing : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, IronCasing::class.instance(), 2)
                .input(ForgeHammer::class.instance())
                .input(IronPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, IronCasing::class.id())
        }
    }
}

/** 铅质外壳 */
@ModItem(name = "lead_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class LeadCasing : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, LeadCasing::class.instance(), 2)
                .input(ForgeHammer::class.instance())
                .input(LeadPlate::class.instance())
                .criterion(hasItem(LeadPlate::class.instance()), conditionsFromItem(LeadPlate::class.instance()))
                .offerTo(exporter, LeadCasing::class.id())
        }
    }
}

/** 钢质外壳 */
@ModItem(name = "steel_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class SteelCasing : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SteelCasing::class.instance(), 2)
                .input(ForgeHammer::class.instance())
                .input(SteelPlate::class.instance())
                .criterion(hasItem(SteelPlate::class.instance()), conditionsFromItem(SteelPlate::class.instance()))
                .offerTo(exporter, SteelCasing::class.id())
        }
    }
}

/** 锡质外壳 */
@ModItem(name = "tin_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class TinCasing : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TinCasing::class.instance(), 2)
                .input(ForgeHammer::class.instance())
                .input(TinPlate::class.instance())
                .criterion(hasItem(TinPlate::class.instance()), conditionsFromItem(TinPlate::class.instance()))
                .offerTo(exporter, TinCasing::class.id())
        }
    }
}

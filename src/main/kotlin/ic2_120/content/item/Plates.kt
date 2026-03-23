package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import java.util.function.Consumer
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem

// ========== 板类（金属成型机切割：1 锭 -> 1 板；青金石/黑曜石见配方） ==========

@ModItem(name = "bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class BronzePlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, BronzePlate::class.instance(), 1)
                .input(ForgeHammer::class.instance())
                .input(BronzeIngot::class.instance())
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzePlate::class.id())
        }
    }
}

@ModItem(name = "copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class CopperPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, CopperPlate::class.instance(), 1)
                .input(Items.COPPER_INGOT)
                .input(ForgeHammer::class.instance())
                .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT))
                .offerTo(exporter, CopperPlate::class.id())
        }
    }
}

@ModItem(name = "gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class GoldPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, GoldPlate::class.instance(), 1)
                .input(Items.GOLD_INGOT)
                .input(ForgeHammer::class.instance())
                .criterion(hasItem(Items.GOLD_INGOT), conditionsFromItem(Items.GOLD_INGOT))
                .offerTo(exporter, GoldPlate::class.id())
        }
    }
}

@ModItem(name = "iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class IronPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, IronPlate::class.instance(), 1)
                .input(ForgeHammer::class.instance())
                .input(Items.IRON_INGOT)
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, IronPlate::class.id())
        }
    }
}

@ModItem(name = "lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class LapisPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, LapisPlate::class.instance(), 1)
                .input(ForgeHammer::class.instance())
                .input(Items.LAPIS_LAZULI)
                .criterion(hasItem(Items.LAPIS_LAZULI), conditionsFromItem(Items.LAPIS_LAZULI))
                .offerTo(exporter, LapisPlate::class.id())
        }
    }
}

@ModItem(name = "lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class LeadPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, LeadPlate::class.instance(), 1)
                .input(ForgeHammer::class.instance())
                .input(LeadIngot::class.instance())
                .criterion(hasItem(LeadIngot::class.instance()), conditionsFromItem(LeadIngot::class.instance()))
                .offerTo(exporter, LeadPlate::class.id())
        }
    }
}

@ModItem(name = "obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class ObsidianPlate : Item(FabricItemSettings())

@ModItem(name = "steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class SteelPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SteelPlate::class.instance(), 1)
                .input(ForgeHammer::class.instance())
                .input(SteelIngot::class.instance())
                .criterion(hasItem(SteelIngot::class.instance()), conditionsFromItem(SteelIngot::class.instance()))
                .offerTo(exporter, SteelPlate::class.id())
        }
    }
}

@ModItem(name = "tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class TinPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TinPlate::class.instance(), 1)
                .input(ForgeHammer::class.instance())
                .input(TinIngot::class.instance())
                .criterion(hasItem(TinIngot::class.instance()), conditionsFromItem(TinIngot::class.instance()))
                .offerTo(exporter, TinPlate::class.id())
        }
    }
}

// ========== 致密板类（压缩机：9 板 -> 1 致密板） ==========

@ModItem(name = "dense_bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseBronzePlate : Item(FabricItemSettings())

@ModItem(name = "dense_copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseCopperPlate : Item(FabricItemSettings())

@ModItem(name = "dense_gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseGoldPlate : Item(FabricItemSettings())

@ModItem(name = "dense_iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseIronPlate : Item(FabricItemSettings())

@ModItem(name = "dense_lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLapisPlate : Item(FabricItemSettings())

@ModItem(name = "dense_lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLeadPlate : Item(FabricItemSettings())

@ModItem(name = "dense_obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseObsidianPlate : Item(FabricItemSettings())

@ModItem(name = "dense_steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseSteelPlate : Item(FabricItemSettings())

@ModItem(name = "dense_tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseTinPlate : Item(FabricItemSettings())

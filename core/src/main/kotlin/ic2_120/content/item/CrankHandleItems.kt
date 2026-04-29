package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.content.recipes.ModTags
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

/**
 * 曲柄材质类型
 */
enum class CrankMaterial(
    val kuPerTick: Int,
    val textureSuffix: String
) {
    WOOD(64, "wood"),
    IRON(128, "iron"),
    STEEL(256, "steel"),
    CARBON(512, "carbon");

    companion object {
        fun fromOrdinal(ordinal: Int): CrankMaterial? =
            entries.getOrNull(ordinal)
    }
}

/**
 * 基础曲柄物品类
 */
abstract class CrankHandleItem(
    val material: CrankMaterial,
    settings: FabricItemSettings = FabricItemSettings()
) : Item(settings) {
    fun getKuPerTick(): Int = material.kuPerTick
}

@ModItem(name = "wooden_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class WoodenCrankHandle : CrankHandleItem(CrankMaterial.WOOD) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val stick = net.minecraft.item.Items.STICK
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WoodenCrankHandle::class.instance(), 1)
                .pattern(" S ")
                .pattern("S S")
                .pattern(" S ")
                .input('S', stick)
                .criterion(hasItem(stick), conditionsFromItem(stick))
                .offerTo(exporter, WoodenCrankHandle::class.id())
        }
    }
}

@ModItem(name = "iron_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class IronCrankHandle : CrankHandleItem(CrankMaterial.IRON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val wooden = WoodenCrankHandle::class.instance()
            val plate = IronPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronCrankHandle::class.instance(), 1)
                .pattern(" P ")
                .pattern("PWP")
                .pattern(" P ")
                .input('W', wooden)
                .input('P', plate)
                .criterion(hasItem(wooden), conditionsFromItem(wooden))
                .offerTo(exporter, IronCrankHandle::class.id())
        }
    }
}

@ModItem(name = "steel_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class SteelCrankHandle : CrankHandleItem(CrankMaterial.STEEL) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val iron = IronCrankHandle::class.instance()
            val plate = SteelPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelCrankHandle::class.instance(), 1)
                .pattern(" P ")
                .pattern("PWP")
                .pattern(" P ")
                .input('W', iron)
                .input('P', plate)
                .criterion(hasItem(iron), conditionsFromItem(iron))
                .offerTo(exporter, SteelCrankHandle::class.id())
        }
    }
}

@ModItem(name = "carbon_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class CarbonCrankHandle : CrankHandleItem(CrankMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val steel = SteelCrankHandle::class.instance()
            val plate = CarbonPlate::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CarbonCrankHandle::class.instance(), 1)
                .pattern(" P ")
                .pattern("PWP")
                .pattern(" P ")
                .input('W', steel)
                .input('P', plate)
                .criterion(hasItem(steel), conditionsFromItem(steel))
                .offerTo(exporter, CarbonCrankHandle::class.id())
        }
    }
}

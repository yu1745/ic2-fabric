package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.content.recipes.ModTags
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.data.recipes.RecipeCategory

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
    settings: Item.Properties = Item.Properties()
) : Item(settings) {
    fun getKuPerTick(): Int = material.kuPerTick
}

@ModItem(name = "wooden_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class WoodenCrankHandle : CrankHandleItem(CrankMaterial.WOOD) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val stick = net.minecraft.world.item.Items.STICK
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, WoodenCrankHandle::class.instance(), 1)
                .pattern(" S ")
                .pattern("S S")
                .pattern(" S ")
                .define('S', stick)
                .unlockedBy(getHasName(stick), has(stick))
                .save(exporter, WoodenCrankHandle::class.id())
        }
    }
}

@ModItem(name = "iron_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class IronCrankHandle : CrankHandleItem(CrankMaterial.IRON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val wooden = WoodenCrankHandle::class.instance()
            val plate = IronPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronCrankHandle::class.instance(), 1)
                .pattern(" P ")
                .pattern("PWP")
                .pattern(" P ")
                .define('W', wooden)
                .define('P', plate)
                .unlockedBy(getHasName(wooden), has(wooden))
                .save(exporter, IronCrankHandle::class.id())
        }
    }
}

@ModItem(name = "steel_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class SteelCrankHandle : CrankHandleItem(CrankMaterial.STEEL) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val iron = IronCrankHandle::class.instance()
            val plate = SteelPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteelCrankHandle::class.instance(), 1)
                .pattern(" P ")
                .pattern("PWP")
                .pattern(" P ")
                .define('W', iron)
                .define('P', plate)
                .unlockedBy(getHasName(iron), has(iron))
                .save(exporter, SteelCrankHandle::class.id())
        }
    }
}

@ModItem(name = "carbon_crank_handle", tab = CreativeTab.IC2_MATERIALS, group = "crank_handles")
class CarbonCrankHandle : CrankHandleItem(CrankMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val steel = SteelCrankHandle::class.instance()
            val plate = CarbonPlate::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonCrankHandle::class.instance(), 1)
                .pattern(" P ")
                .pattern("PWP")
                .pattern(" P ")
                .define('W', steel)
                .define('P', plate)
                .unlockedBy(getHasName(steel), has(steel))
                .save(exporter, CarbonCrankHandle::class.id())
        }
    }
}

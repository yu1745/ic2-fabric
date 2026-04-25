package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.recipe.book.RecipeCategory
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.content.item.IronPlate
import ic2_120.content.item.SteelPlate
import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.Alloy
import ic2_120.registry.annotation.RecipeProvider

/**
 * 基础机械外壳。用于建造机器的结构方块。
 */
@ModBlock(name = "machine", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "machine_casing")
class MachineCasingBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MachineCasingBlock::class.instance(), 1)
                .pattern("III")
                .pattern("I I")
                .pattern("III")
                .input('I', IronPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, MachineCasingBlock::class.recipeId("from_plates"))
        }
    }
}

/**
 * 高级机械外壳。用于建造高级机器的结构方块。
 */
@ModBlock(name = "advanced_machine", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "machine_casing")
class AdvancedMachineCasingBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedMachineCasingBlock::class.instance(), 1)
                .pattern("sgs")
                .pattern("cxc")
                .pattern("sgs")
                .input('s', SteelPlate::class.instance())
                .input('g', Alloy::class.instance())
                .input('c', CarbonPlate::class.instance())
                .input('x', MachineCasingBlock::class.instance())
                .criterion(hasItem(SteelPlate::class.instance()), conditionsFromItem(SteelPlate::class.instance()))
                .offerTo(exporter, AdvancedMachineCasingBlock::class.recipeId("from_components"))
        }
    }
}

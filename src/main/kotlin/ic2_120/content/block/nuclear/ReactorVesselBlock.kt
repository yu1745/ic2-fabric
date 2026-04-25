package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.content.item.LeadPlate
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory

/**
 * 核反应堆压力容器。
 */
@ModBlock(name = "reactor_vessel", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorVesselBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val leadPlate = LeadPlate::class.instance()
            if (leadPlate != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorVesselBlock::class.item(), 4)
                    .pattern("LSL").pattern("SLS").pattern("LSL")
                    .input('L', leadPlate)
                    .input('S', Items.STONE)
                    .criterion(hasItem(leadPlate), conditionsFromItem(leadPlate))
                    .offerTo(exporter, ReactorVesselBlock::class.id())
            }
        }
    }
}

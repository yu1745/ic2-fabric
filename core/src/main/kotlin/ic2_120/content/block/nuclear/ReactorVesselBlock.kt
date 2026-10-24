package ic2_120.content.block.nuclear

import ic2_120.content.block.ReinforcedGlassBlock
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
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import ic2_120.content.recipes.ModTags

/**
 * 核反应堆压力容器。
 */
@ModBlock(name = "reactor_vessel", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "reactor", renderLayer = "translucent")
class ReactorVesselBlock(settings: AbstractBlock.Settings = ReactorShellBlock.shellSettings()) : ReactorShellBlock(settings) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val leadPlate = LeadPlate::class.instance()
            if (leadPlate != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorVesselBlock::class.item(), 4)
                    .pattern("LGL").pattern("GLG").pattern("LGL")
                    .input('L', Ingredient.fromTag(ModTags.Compat.Items.PLATES_LEAD))
                    .input('G', ReinforcedGlassBlock::class.item())  // 透明外壳：铅框 + 强化玻璃
                    .criterion(hasItem(leadPlate), conditionsFromItem(leadPlate))
                    .offerTo(exporter, ReactorVesselBlock::class.id())
            }
        }
    }
}

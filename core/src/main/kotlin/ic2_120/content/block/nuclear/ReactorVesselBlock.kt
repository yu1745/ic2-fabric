package ic2_120.content.block.nuclear

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.content.item.LeadPlate
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Blocks
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import ic2_120.content.recipes.ModTags

/**
 * 核反应堆压力容器。
 */
@ModBlock(name = "reactor_vessel", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "reactor", renderLayer = "translucent")
class ReactorVesselBlock(settings: BlockBehaviour.Properties = ReactorShellBlock.shellSettings()) : ReactorShellBlock(settings) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val leadPlate = LeadPlate::class.instance()
            if (leadPlate != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ReactorVesselBlock::class.item(), 4)
                    .pattern("LGL").pattern("GLG").pattern("LGL")
                    .define('L', Ingredient.of(ModTags.Compat.Items.PLATES_LEAD))
                    .define('G', ReinforcedGlassBlock::class.item())  // 透明外壳：铅框 + 强化玻璃
                    .unlockedBy(getHasName(leadPlate), has(leadPlate))
                    .save(exporter, ReactorVesselBlock::class.id())
            }
        }
    }
}

package ic2_120.content.block

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import ic2_120.content.item.IronPlate
import ic2_120.content.item.SteelPlate
import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.Alloy
import ic2_120.content.recipes.ModTags
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 基础机械外壳。用于建造机器的结构方块。
 */
@ModBlock(name = "machine", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "machine_casing")
class MachineCasingBlock : Block(
    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MachineCasingBlock::class.instance(), 1)
                .pattern("III")
                .pattern("I I")
                .pattern("III")
                .define('I', Ingredient.of(ModTags.Compat.Items.PLATES_IRON))
                .unlockedBy(has(IronPlate::class.instance()), has(IronPlate::class.instance()))
                .save(exporter, MachineCasingBlock::class.recipeId("from_plates"))
        }
    }
}

/**
 * 高级机械外壳。用于建造高级机器的结构方块。
 */
@ModBlock(name = "advanced_machine", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "machine_casing")
class AdvancedMachineCasingBlock : Block(
    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AdvancedMachineCasingBlock::class.instance(), 1)
                .pattern("sgs")
                .pattern("cxc")
                .pattern("sgs")
                .define('s', Ingredient.of(ModTags.Compat.Items.PLATES_STEEL))
                .define('g', Alloy::class.instance())
                .define('c', CarbonPlate::class.instance())
                .define('x', MachineCasingBlock::class.instance())
                .unlockedBy(has(SteelPlate::class.instance()), has(SteelPlate::class.instance()))
                .save(exporter, AdvancedMachineCasingBlock::class.recipeId("from_components"))
        }
    }
}

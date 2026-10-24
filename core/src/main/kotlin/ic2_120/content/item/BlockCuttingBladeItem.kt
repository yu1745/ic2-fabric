package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory

@ModItem(name = "iron_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class IronBlockCuttingBladeItem : Item(Item.Properties()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 5.0f

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val dense = DenseIronPlate::class.instance()
            if (dense != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronBlockCuttingBladeItem::class.instance(), 1)
                    .pattern("PPP").pattern("PSP").pattern("PPP")
                    .define('P', dense).define('S', Items.STONE)
                    .unlockedBy(getHasName(dense), has(dense))
                    .save(exporter, IronBlockCuttingBladeItem::class.id())
            }
        }
    }
}

@ModItem(name = "steel_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class SteelBlockCuttingBladeItem : Item(Item.Properties()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 6.0f

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plate = SteelPlate::class.instance()
            if (plate != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteelBlockCuttingBladeItem::class.instance(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .define('A', Ingredient.of(ModTags.Compat.Items.PLATES_STEEL)).define('B', Ingredient.of(ModTags.Compat.Items.INGOTS_IRON))
                    .unlockedBy(getHasName(plate), has(plate))
                    .save(exporter, SteelBlockCuttingBladeItem::class.id())
            }
        }
    }
}

@ModItem(name = "diamond_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class DiamondBlockCuttingBladeItem : Item(Item.Properties()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 50.0f

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val steel = SteelIngot::class.instance()
            if (steel != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DiamondBlockCuttingBladeItem::class.instance(), 1)
                    .pattern("DDD").pattern("DSD").pattern("DDD")
                    .define('D', Items.DIAMOND).define('S', steel)
                    .unlockedBy(getHasName(steel), has(steel))
                    .save(exporter, DiamondBlockCuttingBladeItem::class.id())
            }
        }
    }
}

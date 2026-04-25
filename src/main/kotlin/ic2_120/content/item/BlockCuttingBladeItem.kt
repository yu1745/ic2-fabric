package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

@ModItem(name = "iron_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class IronBlockCuttingBladeItem : Item(Item.Settings()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 5.0f

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val dense = DenseIronPlate::class.instance()
            if (dense != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronBlockCuttingBladeItem::class.instance(), 1)
                    .pattern("PPP").pattern("PSP").pattern("PPP")
                    .input('P', dense).input('S', Items.STONE)
                    .criterion(hasItem(dense), conditionsFromItem(dense))
                    .offerTo(exporter, IronBlockCuttingBladeItem::class.id())
            }
        }
    }
}

@ModItem(name = "steel_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class SteelBlockCuttingBladeItem : Item(Item.Settings()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 6.0f

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val plate = SteelPlate::class.instance()
            if (plate != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelBlockCuttingBladeItem::class.instance(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .input('A', plate).input('B', Items.IRON_INGOT)
                    .criterion(hasItem(plate), conditionsFromItem(plate))
                    .offerTo(exporter, SteelBlockCuttingBladeItem::class.id())
            }
        }
    }
}

@ModItem(name = "diamond_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class DiamondBlockCuttingBladeItem : Item(Item.Settings()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 50.0f

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val steel = SteelIngot::class.instance()
            if (steel != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, DiamondBlockCuttingBladeItem::class.instance(), 1)
                    .pattern("DDD").pattern("DSD").pattern("DDD")
                    .input('D', Items.DIAMOND).input('S', steel)
                    .criterion(hasItem(steel), conditionsFromItem(steel))
                    .offerTo(exporter, DiamondBlockCuttingBladeItem::class.id())
            }
        }
    }
}

package addon_template.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import addon_template.AddonTemplate
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.minecraft.data.recipes.RecipeProvider.has

@ModItem(name = "example_item", tab = CreativeTab.IC2_MATERIALS, group = "material")
class ExampleItem : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ExampleItem::class.instance(), 1)
                .pattern(" D ")
                .pattern("DID")
                .pattern(" D ")
                .define('D', Items.DIAMOND)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.DIAMOND), has(Items.DIAMOND))
                .save(exporter, AddonTemplate.id("example_item"))
        }
    }
}

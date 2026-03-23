package ic2_120.integration.jei

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 方块切割机 JEI 分类
 */
class BlockCutterRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<BlockCutterJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 50)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "block_cutter")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.BLOCK_CUTTER
    override fun getTitle(): Text = Text.translatable("block.ic2_120.block_cutter")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: BlockCutterJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 16)
            .addIngredients(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 16)
            .addItemStack(recipe.output)
    }
}

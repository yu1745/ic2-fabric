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
 * 简易 JEI 分类：只表达输入、输出与机器类型。
 */
class ExtractorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ExtractorJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "extractor")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.EXTRACTOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.extractor")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: ExtractorJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 18)
            .addItemStack(recipe.input)

        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 18)
            .addItemStack(recipe.output)
    }
}

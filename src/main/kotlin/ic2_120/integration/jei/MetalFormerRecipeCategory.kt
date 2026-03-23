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
 * 金属成型机 JEI 分类 - 辊压模式
 */
class MetalFormerRollingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MetalFormerRollingJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 50)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "metal_former")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.METAL_FORMER_ROLLING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.metal_former.rolling")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MetalFormerRollingJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 16)
            .addIngredients(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 16)
            .addItemStack(recipe.output)
    }
}

/**
 * 金属成型机 JEI 分类 - 切割模式
 */
class MetalFormerCuttingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MetalFormerCuttingJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 50)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "metal_former")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.METAL_FORMER_CUTTING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.metal_former.cutting")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MetalFormerCuttingJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 16)
            .addIngredients(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 16)
            .addItemStack(recipe.output)
    }
}

/**
 * 金属成型机 JEI 分类 - 挤压模式
 */
class MetalFormerExtrudingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MetalFormerExtrudingJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 50)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "metal_former")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.METAL_FORMER_EXTRUDING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.metal_former.extruding")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MetalFormerExtrudingJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 16)
            .addIngredients(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 16)
            .addItemStack(recipe.output)
    }
}

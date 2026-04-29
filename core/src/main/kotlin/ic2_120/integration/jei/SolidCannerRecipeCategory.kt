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
 * 固体装罐机 JEI 分类。
 * 布局：左侧上下两个输入槽，右侧产出槽（与其他机器一致）
 */
class SolidCannerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<SolidCannerJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "solid_canner")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.SOLID_CANNER

    override fun getTitle(): Text = Text.translatable("block.ic2_120.solid_canner")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: SolidCannerJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 左侧上方：槽0（锡罐 或 空燃料棒）
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 9)
            .addItemStack(recipe.slot0)

        // 左侧下方：槽1（食物 或 核燃料）
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 31)
            .addItemStack(recipe.slot1)

        // 右侧：产出
        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 20)
            .addItemStack(recipe.output)
    }
}

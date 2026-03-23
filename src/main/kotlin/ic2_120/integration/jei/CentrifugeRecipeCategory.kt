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
 * 热能离心机 JEI 分类 - 支持多输出显示
 *
 * 布局：
 * - 左侧：1个输入槽
 * - 右侧：3个输出槽（垂直排列）
 * - 中间：显示最低热量要求
 */
class CentrifugeRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<CentrifugeJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 70)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "centrifuge")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.CENTRIFUGE

    override fun getTitle(): Text = Text.translatable("block.ic2_120.centrifuge")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: CentrifugeJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽（左侧）
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 26)
            .addIngredients(recipe.input)

        // 输出槽（右侧，垂直排列）
        recipe.outputs.forEachIndexed { index, output ->
            val y = 10 + index * 20
            builder.addSlot(RecipeIngredientRole.OUTPUT, 102, y)
                .addItemStack(output)
        }
    }
}

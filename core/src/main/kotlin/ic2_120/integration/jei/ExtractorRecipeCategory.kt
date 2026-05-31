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
 * 提取机 JEI 分类 — 使用机器纹理 [guiextractor.png] 作为背景。
 *
 * 裁取区域 (33, 5) → (145, 83)，覆盖提取机面板中
 * 输入槽 (62,16) 与输出槽 (116,35) 所在的工作区域。
 */
class ExtractorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ExtractorJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier("ic2", "textures/gui/guiextractor.png"),
        33, 5, 112, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "extractor")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.EXTRACTOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.extractor")

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: ExtractorJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽 — 与纹理中 (62, 16) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 29, 11)
            .addItemStack(recipe.input)

        // 输出槽 — 与纹理中 (116, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 83, 30)
            .addItemStack(recipe.output)
    }
}

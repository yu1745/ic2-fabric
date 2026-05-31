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
 * 压缩机 JEI 分类 — 使用真实机器纹理 [guicompressor.png] 作为背景。
 *
 * 纹理裁取区域 (30, 3) → (170, 70)，覆盖压缩机主面板中
 * 输入槽 (58,15) 与输出槽 (116,34) 所在的工作区域。
 */
class CompressorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<CompressorJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.of("ic2", "textures/gui/guicompressor.png"),
        30, 5, 118, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "compressor")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.COMPRESSOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.compressor")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: CompressorJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽 — 与纹理中 (58, 15) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 10)
            .addItemStack(recipe.input)

        // 输出槽 — 与纹理中 (116, 34) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 29)
            .addItemStack(recipe.output)

        // 容器返还槽（如水单元→雪块，返还空单元）
        if (!recipe.containerReturn.isEmpty) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 47)
                .addItemStack(recipe.containerReturn)
        }
    }
}

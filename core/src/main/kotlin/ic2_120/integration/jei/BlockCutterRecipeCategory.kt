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
 * 方块切割机 JEI 分类 — 使用机器纹理 [guiblockcuttingmachine.png] 作为背景。
 *
 * 裁取区域 (10, 5) → (140, 83)，覆盖切割机面板中
 * 输入槽 (26,17) 与输出槽 (116,35) 所在的工作区域。
 */
class BlockCutterRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<BlockCutterJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier("ic2", "textures/gui/guiblockcuttingmachine.png"),
        10, 5, 130, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "block_cutter")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.BLOCK_CUTTER
    override fun getTitle(): Text = Text.translatable("block.ic2_120.block_cutter")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: BlockCutterJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽 — 与纹理中 (26, 17) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 12)
            .addItemStack(recipe.input)

        // 输出槽 — 与纹理中 (116, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 30)
            .addItemStack(recipe.output)
    }
}

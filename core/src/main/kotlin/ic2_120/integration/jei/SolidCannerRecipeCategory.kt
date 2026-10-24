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
 * 固体装罐机 JEI 分类 — 使用机器纹理 [guisolidcanner.png] 作为背景。
 *
 * 裁取区域 (25, 21) → (137, 66)，覆盖固体装罐机面板中
 * 锡罐槽 (29,35)、食物槽 (61,35)、输出槽 (117,35) 所在区域。
 */
class SolidCannerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<SolidCannerJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier("ic2", "textures/gui/guisolidcanner.png"),
        25, 21, 112, 45
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "solid_canner")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.SOLID_CANNER

    override fun getTitle(): Text = Text.translatable("block.ic2_120.solid_canner")

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: SolidCannerJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 锡罐槽 — 与纹理中 (29, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 14)
            .addItemStacks(recipe.slot0)

        // 食物槽 — 与纹理中 (61, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 14)
            .addItemStacks(recipe.slot1)

        // 输出槽 — 与纹理中 (117, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 14)
            .addItemStack(recipe.output)
    }
}

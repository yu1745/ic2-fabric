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
 * 回收机 JEI 分类 — 使用机器纹理 [guirecycler.png] 作为背景。
 *
 * 裁取区域 (30, 5) → (150, 83)，覆盖回收机面板中
 * 输入槽 (52,16) 与输出槽 (111,35) 所在的工作区域。
 */
class RecyclerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<RecyclerJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier("ic2", "textures/gui/guirecycler.png"),
        30, 5, 120, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "recycler")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.RECYCLER

    override fun getTitle(): Text = Text.translatable("block.ic2_120.recycler")

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: RecyclerJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽 — 与纹理中 (52, 16) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 22, 11)
            .addItemStack(recipe.input)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120.recycler.input.1"))
                tooltip.add(Text.translatable("jei.ic2_120.recycler.input.2"))
            }

        // 输出槽 — 与纹理中 (111, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 81, 30)
            .addItemStack(recipe.output)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120.recycler.output"))
            }
    }
}

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
 * 热能离心机 JEI 分类 — 使用机器纹理 [guicentrifuge.png] 作为背景。
 *
 * 裁取区域 (0, 8) → (150, 86)，覆盖离心机面板中
 * 输入槽 (11,21) 与 3 个输出槽 (124,18/36/54) 所在的工作区域。
 */
class CentrifugeRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<CentrifugeJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.of("ic2", "textures/gui/guicentrifuge.png"),
        0, 11, 150, 72
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "centrifuge")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.CENTRIFUGE

    override fun getTitle(): Text = Text.translatable("block.ic2_120.centrifuge")

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: CentrifugeJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽（左侧）— 与纹理中 (11, 21) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 10)
            .addItemStack(recipe.input)

        // 输出槽（右侧，垂直排列）— 与纹理中 (124, 18/36/54) 处槽位对齐
        val outputY = intArrayOf(7, 25, 43)
        recipe.outputs.forEachIndexed { index, output ->
            val y = outputY.getOrElse(index) { 10 + index * 18 }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 124, y)
                .addItemStack(output)
        }
    }
}

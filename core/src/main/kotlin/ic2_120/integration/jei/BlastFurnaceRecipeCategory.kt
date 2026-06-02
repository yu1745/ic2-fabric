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
 * 高炉 JEI 分类 — 使用机器纹理 [guiblockcutter.png] 上半部分作为背景。
 *
 * 裁取区域 (0, 0) → (170, 80)，覆盖高炉面板上半部分，
 * 包含压缩空气罐、温度条、热源指示、工作指示等区域。
 */
class BlastFurnaceRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<BlastFurnaceJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.of("ic2", "textures/gui/guiblockcutter.png"),
        0, 0, 170, 80
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "blast_furnace")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.BLAST_FURNACE
    override fun getTitle(): Text = Text.translatable("block.ic2_120.blast_furnace")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: BlastFurnaceJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 33)
            .addItemStacks(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 56)
            .addItemStack(recipe.steelOutput)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 152, 56)
            .addItemStack(recipe.slagOutput)
    }
}

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
import java.text.NumberFormat

<<<<<<< HEAD
class ReplicatorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ReplicatorJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "replicator")))
=======
<<<<<<<< HEAD:core/src/main/kotlin/ic2_120/integration/jei/MaceratorRecipeCategory.kt
/**
 * 打粉机 JEI 分类 — 使用机器纹理 [scrapboxrecipes.png] 作为背景。
 *
 * 裁取区域 (26, 5) → (150, 83)，覆盖打粉机面板中
 * 输入槽 (42,15) 与输出槽 (116,35) 所在的工作区域。
 */
class MaceratorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MaceratorJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier("ic2", "textures/gui/scrapboxrecipes.png"),
        26, 5, 124, 78
    )
========
class ReplicatorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ReplicatorJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
>>>>>>>> backup/1.21.1:core/src/main/kotlin/ic2_120/integration/jei/ReplicatorRecipeCategory.kt
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "replicator")))
>>>>>>> backup/1.21.1
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.REPLICATOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.replicator")

<<<<<<< HEAD
    // JEI 15.20.0 起 getBackground() 已 @Deprecated(forRemoval)，替代方案需 override draw()，
    // 但 draw() 形参 DrawContext 是 client-only 类，无法在主源集引用，故沿用 getBackground() + suppress。
=======
>>>>>>> backup/1.21.1
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: ReplicatorJeiRecipe,
        focuses: IFocusGroup
    ) {
<<<<<<< HEAD
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 18)
=======
<<<<<<<< HEAD:core/src/main/kotlin/ic2_120/integration/jei/MaceratorRecipeCategory.kt
        // 输入槽 — 与纹理中 (42, 15) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 10)
            .addItemStacks(recipe.input)

        // 输出槽 — 与纹理中 (116, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 30)
========
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 18)
>>>>>>>> backup/1.21.1:core/src/main/kotlin/ic2_120/integration/jei/ReplicatorRecipeCategory.kt
>>>>>>> backup/1.21.1
            .addItemStack(recipe.output)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120.replicator.uu_cost", NumberFormat.getIntegerInstance().format(recipe.uuCostUb)))
            }
    }
}

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
<<<<<<< HEAD
import java.text.NumberFormat

<<<<<<<< HEAD:core/src/main/kotlin/ic2_120/integration/jei/MaceratorRecipeCategory.kt
=======

>>>>>>> backup/1.21.1
/**
 * 打粉机 JEI 分类 — 使用机器纹理 [scrapboxrecipes.png] 作为背景。
 *
 * 裁取区域 (26, 5) → (150, 83)，覆盖打粉机面板中
 * 输入槽 (42,15) 与输出槽 (116,35) 所在的工作区域。
 */
class MaceratorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MaceratorJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
<<<<<<< HEAD
        Identifier("ic2", "textures/gui/scrapboxrecipes.png"),
        26, 5, 124, 78
    )
========
class ReplicatorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ReplicatorJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
>>>>>>>> backup/1.21.1:core/src/main/kotlin/ic2_120/integration/jei/ReplicatorRecipeCategory.kt
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "replicator")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.REPLICATOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.replicator")
=======
        Identifier.of("ic2", "textures/gui/scrapboxrecipes.png"),
        26, 5, 124, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "macerator")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.MACERATOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.macerator")
>>>>>>> backup/1.21.1

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
<<<<<<< HEAD
        recipe: ReplicatorJeiRecipe,
        focuses: IFocusGroup
    ) {
<<<<<<<< HEAD:core/src/main/kotlin/ic2_120/integration/jei/MaceratorRecipeCategory.kt
=======
        recipe: MaceratorJeiRecipe,
        focuses: IFocusGroup
    ) {
>>>>>>> backup/1.21.1
        // 输入槽 — 与纹理中 (42, 15) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 10)
            .addItemStacks(recipe.input)

        // 输出槽 — 与纹理中 (116, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 30)
<<<<<<< HEAD
========
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 18)
>>>>>>>> backup/1.21.1:core/src/main/kotlin/ic2_120/integration/jei/ReplicatorRecipeCategory.kt
            .addItemStack(recipe.output)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120.replicator.uu_cost", NumberFormat.getIntegerInstance().format(recipe.uuCostUb)))
            }
=======
            .addItemStack(recipe.output)
>>>>>>> backup/1.21.1
    }
}

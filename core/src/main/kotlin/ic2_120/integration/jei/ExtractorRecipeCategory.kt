<<<<<<< HEAD
package ic2_120_advanced_solar_addon.integration.jei
=======
package ic2_120.integration.jei
>>>>>>> backup/1.21.1

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

<<<<<<<< HEAD:core/src/main/kotlin/ic2_120/integration/jei/ExtractorRecipeCategory.kt
=======

>>>>>>> backup/1.21.1
/**
 * 提取机 JEI 分类 — 使用机器纹理 [guiextractor.png] 作为背景。
 *
 * 裁取区域 (33, 5) → (145, 83)，覆盖提取机面板中
 * 输入槽 (62,16) 与输出槽 (116,35) 所在的工作区域。
 */
class ExtractorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ExtractorJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
<<<<<<< HEAD
        Identifier("ic2", "textures/gui/guiextractor.png"),
        33, 5, 112, 78
    )
========
class MolecularTransformerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MolecularTransformerJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
>>>>>>>> backup/1.21.1:advanced-solar-addon/src/main/kotlin/ic2_120_advanced_solar_addon/integration/jei/MolecularTransformerRecipeCategory.kt
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120_advanced_solar_addon", "molecular_transformer")))
    )

    override fun getRecipeType() = Ic2AdvancedSolarAddonJeiRecipeTypes.MOLECULAR_TRANSFORMER

    override fun getTitle(): Text = Text.translatable("block.ic2_120_advanced_solar_addon.molecular_transformer")
=======
        Identifier.of("ic2", "textures/gui/guiextractor.png"),
        33, 5, 112, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "extractor")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.EXTRACTOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.extractor")
>>>>>>> backup/1.21.1

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
<<<<<<< HEAD
        recipe: MolecularTransformerJeiRecipe,
=======
        recipe: ExtractorJeiRecipe,
>>>>>>> backup/1.21.1
        focuses: IFocusGroup
    ) {
        // 输入槽 — 与纹理中 (62, 16) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 29, 11)
            .addItemStacks(recipe.input)

        // 输出槽 — 与纹理中 (116, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 83, 30)
            .addItemStack(recipe.output)
<<<<<<< HEAD
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120_advanced_solar_addon.molecular_transformer.energy", NumberFormat.getIntegerInstance().format(recipe.energy)))
            }
=======
>>>>>>> backup/1.21.1
    }
}

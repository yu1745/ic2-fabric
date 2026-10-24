package ic2_120_advanced_solar_addon.integration.jei

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
<<<<<<<< HEAD:advanced-solar-addon/src/main/kotlin/ic2_120_advanced_solar_addon/integration/jei/MolecularTransformerRecipeCategory.kt
class MolecularTransformerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MolecularTransformerJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120_advanced_solar_addon", "molecular_transformer")))
========
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
>>>>>>>> backup/1.21.1:core/src/main/kotlin/ic2_120/integration/jei/CompressorRecipeCategory.kt
=======
<<<<<<<< HEAD:core/src/main/kotlin/ic2_120/integration/jei/ExtractorRecipeCategory.kt
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
========
class MolecularTransformerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MolecularTransformerJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
>>>>>>>> backup/1.21.1:advanced-solar-addon/src/main/kotlin/ic2_120_advanced_solar_addon/integration/jei/MolecularTransformerRecipeCategory.kt
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120_advanced_solar_addon", "molecular_transformer")))
>>>>>>> backup/1.21.1
    )

    override fun getRecipeType() = Ic2AdvancedSolarAddonJeiRecipeTypes.MOLECULAR_TRANSFORMER

    override fun getTitle(): Text = Text.translatable("block.ic2_120_advanced_solar_addon.molecular_transformer")

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MolecularTransformerJeiRecipe,
        focuses: IFocusGroup
    ) {
<<<<<<< HEAD
<<<<<<<< HEAD:advanced-solar-addon/src/main/kotlin/ic2_120_advanced_solar_addon/integration/jei/MolecularTransformerRecipeCategory.kt
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 18)
            .addItemStack(recipe.input)

        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 18)
=======
        // 输入槽 — 与纹理中 (62, 16) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 29, 11)
            .addItemStacks(recipe.input)

        // 输出槽 — 与纹理中 (116, 35) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 83, 30)
>>>>>>> backup/1.21.1
            .addItemStack(recipe.output)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120_advanced_solar_addon.molecular_transformer.energy", NumberFormat.getIntegerInstance().format(recipe.energy)))
            }
<<<<<<< HEAD
========
        // 输入槽 — 与纹理中 (58, 15) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 10)
            .addItemStacks(recipe.input)

        // 输出槽 — 与纹理中 (116, 34) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 29)
            .addItemStack(recipe.output)

        // 容器返还槽（如水单元→雪块，返还空单元）
        if (!recipe.containerReturn.isEmpty) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 47)
                .addItemStack(recipe.containerReturn)
        }
>>>>>>>> backup/1.21.1:core/src/main/kotlin/ic2_120/integration/jei/CompressorRecipeCategory.kt
=======
>>>>>>> backup/1.21.1
    }
}

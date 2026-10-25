package ic2_120.integration.jei

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * 压缩机 JEI 分类 — 使用真实机器纹理 [guicompressor.png] 作为背景。
 *
 * 纹理裁取区域 (30, 3) → (170, 70)，覆盖压缩机主面板中
 * 输入槽 (58,15) 与输出槽 (116,34) 所在的工作区域。
 */
class CompressorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<CompressorJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.fromNamespaceAndPath("ic2", "textures/gui/guicompressor.png"),
        30, 5, 118, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "compressor")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.COMPRESSOR

    override fun getTitle(): Component = Component.translatable("block.ic2_120.compressor")

    override fun getWidth(): Int = background.width
    override fun getHeight(): Int = background.height
    override fun draw(recipe: CompressorJeiRecipe, recipeSlotsView: mezz.jei.api.gui.ingredient.IRecipeSlotsView, guiGraphics: net.minecraft.client.gui.GuiGraphicsExtractor, mouseX: Double, mouseY: Double) {
        background.draw(guiGraphics)
    }

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: CompressorJeiRecipe,
        focuses: IFocusGroup
    ) {
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
    }
}

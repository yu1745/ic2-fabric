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
 * 金属成型机 JEI 分类 - 辊压模式 — 使用机器纹理 [guimetalformer.png]。
 *
 * 裁取区域 (4, 5) → (150, 83)，覆盖输入槽 (17,17) 与输出槽 (116,35)。
 */
class MetalFormerRollingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MetalFormerRollingJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.of("ic2", "textures/gui/guimetalformer.png"),
        4, 5, 146, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "metal_former")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.METAL_FORMER_ROLLING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.metal_former.rolling")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MetalFormerRollingJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 13, 12)
            .addItemStacks(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 30)
            .addItemStack(recipe.output)
    }
}

/**
 * 金属成型机 JEI 分类 - 切割模式 — 使用机器纹理 [guimetalformer.png]。
 *
 * 裁取区域 (4, 5) → (150, 83)，覆盖输入槽 (17,17) 与输出槽 (116,35)。
 */
class MetalFormerCuttingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MetalFormerCuttingJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.of("ic2", "textures/gui/guimetalformer.png"),
        4, 5, 146, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "metal_former")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.METAL_FORMER_CUTTING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.metal_former.cutting")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MetalFormerCuttingJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 13, 12)
            .addItemStacks(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 30)
            .addItemStack(recipe.output)
    }
}

/**
 * 金属成型机 JEI 分类 - 挤压模式 — 使用机器纹理 [guimetalformer.png]。
 *
 * 裁取区域 (4, 5) → (150, 83)，覆盖输入槽 (17,17) 与输出槽 (116,35)。
 */
class MetalFormerExtrudingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MetalFormerExtrudingJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.of("ic2", "textures/gui/guimetalformer.png"),
        4, 5, 146, 78
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "metal_former")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.METAL_FORMER_EXTRUDING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.metal_former.extruding")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MetalFormerExtrudingJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 13, 12)
            .addItemStacks(recipe.input)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 30)
            .addItemStack(recipe.output)
    }
}

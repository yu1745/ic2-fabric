package ic2_120.integration.jei

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 流体/固体装罐机（ENRICH_LIQUID 模式）JEI 分类 — 使用机器纹理 [guicanner.png]。
 *
 * 裁取区域 (33, 8) → (137, 98)，覆盖装罐机面板中
 * 顶部两个 cell 槽 (41,16)/(119,16)、材料槽 (80,43)、
 * 左右流体槽 (43,46)/(121,46) 及模式指示 (63,80) 所在区域。
 */
class CannerMixingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<CannerMixingJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier.of("ic2", "textures/gui/guicanner.png"),
        33, 8, 104, 90
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "canner")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.CANNER_MIXING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.canner.mixing")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: CannerMixingJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入流体 — 左侧流体罐
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 38)
            .addFluidStack(recipe.inputFluid, FluidConstants.BUCKET)
            .setFluidRenderer(FluidConstants.BUCKET, false, 12, 47)

        // 材料物品 — 中间材料槽
        builder.addSlot(RecipeIngredientRole.INPUT, 47, 35)
            .addItemStacks(recipe.inputSolid)

        // 输出流体 — 右侧流体罐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 88, 38)
            .addFluidStack(recipe.outputFluid, FluidConstants.BUCKET)
            .setFluidRenderer(FluidConstants.BUCKET, false, 12, 47)

        // Keep JEI item lookup connected to the filled cells AND buckets while rendering the visible slots as tanks.
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
            .addItemStacks(buildList {
                recipe.inputFluidCell?.let { add(it) }
                recipe.inputBucket?.let { add(it) }
            })
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStacks(buildList {
                recipe.outputFluidCell?.let { add(it) }
                recipe.outputBucket?.let { add(it) }
            })
    }
}

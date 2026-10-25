package ic2_120.integration.jei

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * AE2 TransformCategory 风格的 common JEI 分类。
 * 水仍然使用现有 JEI/Fabric 流体渲染器，箭头和槽位使用 JEI 布局背景绘制，
 * 因此不需要把这个分类拆到 client 源集。
 */
class CoalWaterTransformRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<CoalWaterTransformJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(130, 62)
    private val slotBackground: IDrawable = guiHelper.createDrawable(
        Identifier.fromNamespaceAndPath("ic2_120", "textures/gui/jei_transform.png"), 0, 34, 18, 18
    )
    private val arrow: IDrawable = guiHelper.createDrawable(
        Identifier.fromNamespaceAndPath("ic2_120", "textures/gui/jei_transform.png"), 0, 17, 24, 17
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ic2_120", "coal_fuel_dust")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.COAL_WATER_TRANSFORM
    override fun getTitle(): Component = Component.translatable("jei.ic2_120.coal_water_transform.title")

    override fun getWidth(): Int = background.width
    override fun getHeight(): Int = background.height
    override fun draw(recipe: CoalWaterTransformJeiRecipe, recipeSlotsView: mezz.jei.api.gui.ingredient.IRecipeSlotsView, guiGraphics: net.minecraft.client.gui.GuiGraphicsExtractor, mouseX: Double, mouseY: Double) {
        background.draw(guiGraphics)
    }

    override fun getIcon(): IDrawable = icon

    @Suppress("DEPRECATION", "REMOVAL")
    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: CoalWaterTransformJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 24)
            .setBackground(slotBackground, -1, -1)
            .setOverlay(arrow, 19, -1)
            .addItemStack(recipe.input)

        builder.addSlot(RecipeIngredientRole.INPUT, 56, 24)
            .setBackground(slotBackground, -1, -1)
            .setOverlay(arrow, 20, -1)
            .addFluidStack(Fluids.WATER, FluidConstants.BUCKET)
            .setFluidRenderer(FluidConstants.BUCKET, false, 16, 16)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.clear()
                tooltip.add(Component.translatable("jei.ic2_120.coal_water_transform.tooltip"))
            }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 24)
            .setBackground(slotBackground, -1, -1)
            .addItemStack(recipe.output)
    }
}

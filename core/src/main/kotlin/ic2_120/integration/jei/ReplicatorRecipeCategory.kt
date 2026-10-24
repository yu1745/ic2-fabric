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
import net.minecraft.resources.ResourceLocation
import java.text.NumberFormat

class ReplicatorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ReplicatorJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.of("ic2_120", "replicator")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.REPLICATOR

    override fun getTitle(): Component = Component.translatable("block.ic2_120.replicator")

    // JEI 15.20.0 起 getBackground() 已 @Deprecated(forRemoval)，替代方案需 override draw()，
    // 但 draw() 形参 GuiGraphics 是 client-only 类，无法在主源集引用，故沿用 getBackground() + suppress。
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: ReplicatorJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 18)
            .addItemStack(recipe.output)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Component.translatable("jei.ic2_120.replicator.uu_cost", NumberFormat.getIntegerInstance().format(recipe.uuCostUb)))
            }
    }
}

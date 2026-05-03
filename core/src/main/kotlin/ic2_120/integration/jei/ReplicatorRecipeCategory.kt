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

class ReplicatorRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<ReplicatorJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "replicator")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.REPLICATOR

    override fun getTitle(): Text = Text.translatable("block.ic2_120.replicator")

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
                tooltip.add(Text.translatable("jei.ic2_120.replicator.uu_cost", NumberFormat.getIntegerInstance().format(recipe.uuCostUb)))
            }
    }
}

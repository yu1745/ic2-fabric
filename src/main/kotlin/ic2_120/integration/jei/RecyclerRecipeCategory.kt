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

class RecyclerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<RecyclerJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "recycler")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.RECYCLER

    override fun getTitle(): Text = Text.translatable("block.ic2_120.recycler")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: RecyclerJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 18)
            .addItemStack(recipe.input)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120.recycler.input.1"))
                tooltip.add(Text.translatable("jei.ic2_120.recycler.input.2"))
            }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 18)
            .addItemStack(recipe.output)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Text.translatable("jei.ic2_120.recycler.output"))
            }
    }
}

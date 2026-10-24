package ic2_120_advanced_solar_addon.integration.jei

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

class MolecularTransformerRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<MolecularTransformerJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 54)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120_advanced_solar_addon", "molecular_transformer")))
    )

    override fun getRecipeType() = Ic2AdvancedSolarAddonJeiRecipeTypes.MOLECULAR_TRANSFORMER

    override fun getTitle(): Component = Component.translatable("block.ic2_120_advanced_solar_addon.molecular_transformer")

    override fun getBackground(): IDrawable = background

    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: MolecularTransformerJeiRecipe,
        focuses: IFocusGroup
    ) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 18)
            .addItemStack(recipe.input)

        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 18)
            .addItemStack(recipe.output)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(Component.translatable("jei.ic2_120_advanced_solar_addon.molecular_transformer.energy", NumberFormat.getIntegerInstance().format(recipe.energy)))
            }
    }
}

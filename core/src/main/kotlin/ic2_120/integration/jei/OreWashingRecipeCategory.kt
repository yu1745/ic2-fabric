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
 * 洗矿机 JEI 分类 - 多输出显示
 */
class OreWashingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<OreWashingJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 70)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "ore_washing_plant")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.ORE_WASHING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.ore_washing_plant")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: OreWashingJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽（左侧）
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 26)
            .addItemStack(recipe.input)

        // 输出槽（右侧，垂直排列）
        recipe.outputs.forEachIndexed { index, output ->
            val y = 10 + index * 20
            builder.addSlot(RecipeIngredientRole.OUTPUT, 102, y)
                .addItemStack(output)
        }
    }
}

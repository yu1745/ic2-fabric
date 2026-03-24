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
 * 高炉 JEI 分类 - 双输出显示
 */
class BlastFurnaceRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<BlastFurnaceJeiRecipe> {
    private val background: IDrawable = guiHelper.createBlankDrawable(140, 50)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "blast_furnace")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.BLAST_FURNACE
    override fun getTitle(): Text = Text.translatable("block.ic2_120.blast_furnace")
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: BlastFurnaceJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 输入槽（左侧）
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 16)
            .addItemStack(recipe.input)

        // 钢锭输出（右侧中间）
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 8)
            .addItemStack(recipe.steelOutput)

        // 炉渣输出（右侧下方）
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 28)
            .addItemStack(recipe.slagOutput)
    }
}

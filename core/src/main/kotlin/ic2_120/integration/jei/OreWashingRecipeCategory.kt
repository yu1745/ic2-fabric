package ic2_120.integration.jei

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.fluid.Fluids
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 洗矿机 JEI 分类 — 使用机器纹理 [guiorewashingplant.png] 作为背景。
 *
 * 裁取区域 (4, 6) → (148, 81)，覆盖洗矿机面板中
 * 矿物输入槽 (104,16) 与底部 3 个输出槽 (86/104/122, 61) 所在的工作区域。
 * 水消耗通过流体槽展示（与纹理中水罐位置对齐）。
 */
class OreWashingRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<OreWashingJeiRecipe> {
    private val background: IDrawable = guiHelper.createDrawable(
        Identifier("ic2", "textures/gui/guiorewashingplant.png"),
        4, 6, 144, 75
    )
    private val icon: IDrawable = guiHelper.createDrawableItemStack(
        ItemStack(Registries.ITEM.get(Identifier("ic2_120", "ore_washing_plant")))
    )

    override fun getRecipeType() = Ic2JeiRecipeTypes.ORE_WASHING
    override fun getTitle(): Text = Text.translatable("block.ic2_120.ore_washing_plant")
    @Deprecated("override deprecated member", level = DeprecationLevel.WARNING)
    override fun getBackground(): IDrawable = background
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: OreWashingJeiRecipe,
        focuses: IFocusGroup
    ) {
        // 水消耗 — 与纹理中水罐 (64,23) 处对齐，尺寸 12×47
        // 配方存储 mB，JEI Fabric 使用 droplets，需转换
        val waterDroplets = recipe.waterConsumptionDroplets * (FluidConstants.BUCKET / 1000)
        builder.addSlot(RecipeIngredientRole.INPUT, 60, 17)
            .addFluidStack(Fluids.WATER, waterDroplets)
            .setFluidRenderer(FluidConstants.BUCKET * 8, false, 12, 47)

        // 矿物输入槽 — 与纹理中 (104, 16) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.INPUT, 100, 10)
            .addItemStacks(recipe.input)

        // 输出槽（底部一行 3 个）— 与纹理中 (86/104/122, 61) 处槽位对齐
        builder.addSlot(RecipeIngredientRole.OUTPUT, 82, 55)
            .addItemStack(recipe.outputs.getOrElse(0) { ItemStack.EMPTY })
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 55)
            .addItemStack(recipe.outputs.getOrElse(1) { ItemStack.EMPTY })
        builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 55)
            .addItemStack(recipe.outputs.getOrElse(2) { ItemStack.EMPTY })
    }
}

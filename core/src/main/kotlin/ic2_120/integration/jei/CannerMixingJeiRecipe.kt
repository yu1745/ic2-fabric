package ic2_120.integration.jei

import net.minecraft.fluid.Fluid
import net.minecraft.item.ItemStack

/**
 * 流体/固体装罐机（ENRICH_LIQUID 模式）JEI 展示配方。
 *
 * @param inputFluid   左侧流体罐代表的输入流体
 * @param inputSolid   中间材料槽的物品
 * @param outputFluid  右侧流体罐代表的输出流体
 * @param inputFluidCell  输入流体对应的满单元（用于 JEI 物品用途查询，nullable 表示无专用单元）
 * @param outputFluidCell 输出流体对应的满单元（用于 JEI 物品配方查询，nullable 表示无专用单元）
 * @param inputBucket   输入流体对应的满桶（nullable：原版水/熔岩桶或 mod 桶；为 null 时 JEI 不会按桶查）
 * @param outputBucket  输出流体对应的满桶（nullable 同上）
 */
data class CannerMixingJeiRecipe(
    val inputFluid: Fluid,
    val inputSolid: List<ItemStack>,
    val outputFluid: Fluid,
    val inputFluidCell: ItemStack?,
    val outputFluidCell: ItemStack?,
    val inputBucket: ItemStack?,
    val outputBucket: ItemStack?
)

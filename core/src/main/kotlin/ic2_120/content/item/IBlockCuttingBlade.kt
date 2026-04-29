package ic2_120.content.item

import net.minecraft.item.ItemStack

/**
 * 方块切割机锯片接口。
 * 锯片无耐久，可无限使用。
 * 硬度决定可切割的材料：只能切割硬度低于锯片硬度的材料。
 */
interface IBlockCuttingBlade {

    /**
     * 锯片硬度。材料硬度必须低于此值才能被切割。
     * 参考：铁 5.0，钢 6.0，钻石 50.0
     */
    fun getBladeHardness(stack: ItemStack): Float
}

package ic2_120.content.block

import ic2_120.registry.item
import net.minecraft.item.ItemStack

/**
 * 供 Mixin 调用的采矿管道判断工具。
 * 客户端 SimpleInventory 会通过此方法判断是否跳过 capCount 截断。
 */
object MiningPipeHelper {
    @JvmStatic
    fun isMiningPipe(stack: ItemStack): Boolean {
        return !stack.isEmpty && stack.item === MiningPipeBlock::class.item()
    }
}

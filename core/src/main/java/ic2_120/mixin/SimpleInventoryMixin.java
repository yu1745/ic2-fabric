package ic2_120.mixin;

import ic2_120.content.block.MiningPipeHelper;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft 1.20.1 SimpleInventory.setStack 会检查堆叠数是否超过 getMaxCountPerStack，
 * 超过则调用 stack.setCount(max) 截断到 64。
 *
 * 对采矿管道跳过此截断，保留服务端同步的真实数量（1000+）。
 * 非管道物品不受影响，仍正常截断。
 * 玩家背包（PlayerInventory）不使用 SimpleInventory，不受影响。
 */
@Mixin(SimpleInventory.class)
public abstract class SimpleInventoryMixin {

    @Redirect(
        method = "setStack(ILnet/minecraft/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;setCount(I)V"
        )
    )
    private void redirectSetCount(ItemStack stack, int count) {
        if (MiningPipeHelper.isMiningPipe(stack)) {
            // 采矿管道：不截断，保留真实数量
            return;
        }
        // 其他物品：正常截断
        stack.setCount(count);
    }
}

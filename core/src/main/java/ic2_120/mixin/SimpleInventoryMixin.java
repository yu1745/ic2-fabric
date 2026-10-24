package ic2_120.mixin;

import ic2_120.content.block.MiningPipeHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft 1.20.1 SimpleContainer.setStack 会检查堆叠数是否超过 getMaxCountPerStack，
 * 超过则调用 stack.setCount(max) 截断到 64。
 *
 * 对采矿管道跳过此截断，保留服务端同步的真实数量（1000+）。
 * 非管道物品不受影响，仍正常截断。
 * 玩家背包（Inventory）不使用 SimpleContainer，不受影响。
 */
@Mixin(SimpleContainer.class)
public abstract class SimpleInventoryMixin {

    @Redirect(
        method = "setItem(ILnet/minecraft/world/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;setCount(I)V"
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

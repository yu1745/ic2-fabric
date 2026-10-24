package ic2_120.mixin;

import ic2_120.content.item.IridiumDrill;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁止铱钻头（IridiumDrill）在砂轮（Grindstone）中被处理，封堵无限刷经验漏洞。
 *
 * 背景：IridiumDrill 的虚拟附魔（Fortune III + Efficiency III，精准模式为 Silk Touch I）
 * 以真实附魔 NBT 承载（供原版挖掘掉落逻辑生效）。若允许砂轮移除附魔：
 * 1) 砂轮按附魔返还经验（每个附魔 level+1）；
 * 2) 钻头返回玩家背包后，inventoryTick 会自动重新写入虚拟附魔；
 * 3) 可无限循环刷经验（砂轮不耗电、钻头无损）。
 *
 * 修复：输入槽含 IridiumDrill 时，输出槽置空并跳过 updateResult——玩家无法取出结果，
 * 经验结算（onTake/removed）随之不会触发。铱钻头本无法通过砂轮持久去附魔
 * （虚拟附魔会自动恢复），禁止处理无正当用途损失。
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneScreenHandlerMixin {

    @Shadow
    @Final
    protected Container input;

    @Shadow
    @Final
    protected Container result;

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void ic2BlockIridiumDrillGrinding(CallbackInfo ci) {
        if (this.input.getStack(0).getItem() instanceof IridiumDrill
            || this.input.getStack(1).getItem() instanceof IridiumDrill) {
            this.result.setStack(0, ItemStack.EMPTY);
            ci.cancel();
        }
    }
}

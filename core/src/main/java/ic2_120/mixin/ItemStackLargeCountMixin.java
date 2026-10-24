package ic2_120.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 允许物品堆叠数量突破 short 上限（大数量反应堆元件缓存）。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackLargeCountMixin {

    @Shadow
    private int count;

    @Inject(method = "<init>(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void ic2ReadLargeCount(HolderLookup.Provider provider, CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("Count")) {
            this.count = nbt.getInt("Count");
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void ic2WriteLargeCount(HolderLookup.Provider provider, CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putInt("Count", this.count);
    }
}

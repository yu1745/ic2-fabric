package ic2_120.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackLargeCountMixin {
    @Shadow
    private int count;

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void ic2ReadLargeCount(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("Count")) {
            this.count = nbt.getInt("Count");
        }
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void ic2WriteLargeCount(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putInt("Count", this.count);
    }
}

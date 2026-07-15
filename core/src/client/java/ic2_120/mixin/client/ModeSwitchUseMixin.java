package ic2_120.mixin.client;

import ic2_120.client.ModeKeybinds;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Consumes Alt+use before vanilla can continue into block or item use. */
@Mixin(MinecraftClient.class)
public abstract class ModeSwitchUseMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void ic2_120$handleModeSwitchUse(CallbackInfo ci) {
        if (ModeKeybinds.tryHandleModeSwitchUse()) {
            ci.cancel();
        }
    }
}

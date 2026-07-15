package ic2_120.mixin.client;

import ic2_120.client.ModeKeybinds;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps modifier state in sync with actual keyboard events for mode-switch interactions. */
@Mixin(Keyboard.class)
public abstract class KeyboardModifierStateMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void ic2_120$trackModifierState(
        long window,
        int key,
        int scancode,
        int action,
        int modifiers,
        CallbackInfo ci
    ) {
        if (window == MinecraftClient.getInstance().getWindow().getHandle()) {
            ModeKeybinds.onKeyEvent(key, action);
        }
    }
}

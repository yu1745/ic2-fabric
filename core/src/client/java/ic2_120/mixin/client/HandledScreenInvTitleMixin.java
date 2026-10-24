package ic2_120.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 将模组 Screen 自带的标题与 "Inv" 文本移到屏幕外，避免遮挡自定义 UI。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenInvTitleMixin {

    @Shadow
    private int titleLabelX;

    @Shadow
    private int titleLabelY;

    @Shadow
    private int inventoryLabelX;

    @Shadow
    private int inventoryLabelY;

    @Inject(method = "init", at = @At("TAIL"))
    private void moveTitlesOffScreen(CallbackInfo ci) {
        Object self = this;
        if (self.getClass().getName().startsWith("ic2_120.client.screen.")) {
            titleLabelX = -1000;
            titleLabelY = -1000;
            inventoryLabelX = -1000;
            inventoryLabelY = -1000;
        }
    }
}

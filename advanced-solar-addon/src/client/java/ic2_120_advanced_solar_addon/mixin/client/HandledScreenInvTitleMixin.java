package ic2_120_advanced_solar_addon.mixin.client;

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
    private int titleLabelY;

    @Shadow
    private int inventoryLabelY;

    @Inject(method = "init", at = @At("TAIL"))
    private void moveTitlesOffScreen(CallbackInfo ci) {
        Object self = this;
        if (self.getClass().getName().startsWith("ic2_120_advanced_solar_addon.client.screen.")) {
            titleLabelY = -1000;
            inventoryLabelY = -1000;
        }
    }
}

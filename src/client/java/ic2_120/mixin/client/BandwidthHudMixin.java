package ic2_120.mixin.client;

import ic2_120.client.network.BandwidthHudState;
import ic2_120.content.network.BandwidthPlayerStat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(InGameHud.class)
public abstract class BandwidthHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "render", at = @At("RETURN"))
    private void ic2_120$renderBandwidthHud(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (client.options.debugEnabled || client.options.hudHidden || !BandwidthHudState.INSTANCE.getEnabled()) {
            return;
        }

        var tr = client.textRenderer;
        int x = 6;
        int y = 6;
        int line = 10;

        long serverBps = BandwidthHudState.INSTANCE.getServerBytesPerSecond();
        context.drawText(tr, Text.literal("IC2 Net Out: " + formatBps(serverBps)), x, y, 0xFF8FE38F, true);
        y += line;

        List<BandwidthPlayerStat> players = BandwidthHudState.INSTANCE.getPlayers();
        int shown = Math.min(players.size(), 8);
        for (int i = 0; i < shown; i++) {
            BandwidthPlayerStat stat = players.get(i);
            String text = stat.getName() + ": " + formatBps(stat.getBytesPerSecond());
            context.drawText(tr, Text.literal(text), x, y, 0xFFFFFFFF, true);
            y += line;
        }
    }

    private static String formatBps(long bytesPerSecond) {
        if (bytesPerSecond < 1024L) return bytesPerSecond + " B/s";
        if (bytesPerSecond < 1024L * 1024L) return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        return String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0));
    }
}

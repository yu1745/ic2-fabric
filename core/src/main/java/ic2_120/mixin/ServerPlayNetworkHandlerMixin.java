package ic2_120.mixin;

import ic2_120.content.network.BandwidthStatsService;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.server.network.ServerCommonNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void ic2_120$recordOutgoingPacket(Packet<?> packet, CallbackInfo ci) {
        if ((Object) this instanceof PlayerAssociatedNetworkHandler handler) {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                BandwidthStatsService.recordPacket(player, packet);
            }
        }
    }
}

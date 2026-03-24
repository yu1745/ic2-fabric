package ic2_120.mixin;

import ic2_120.content.network.BandwidthStatsService;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    @Final
    public ServerPlayerEntity player;

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void ic2_120$recordOutgoingPacket(Packet<?> packet, CallbackInfo ci) {
        BandwidthStatsService.recordPacket(this.player, packet);
    }
}

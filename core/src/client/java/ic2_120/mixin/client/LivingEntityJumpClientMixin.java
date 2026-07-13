package ic2_120.mixin.client;

import ic2_120.content.item.armor.QuantumBoots;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端只预测大跳速度和本地电量；服务端通过原版玩家跳跃调用独立验证并激活落地保护。
 */
@Mixin(LivingEntity.class)
public class LivingEntityJumpClientMixin {

    @Inject(method = "jump", at = @At("TAIL"))
    private void ic2$onClientJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ClientPlayerEntity player)) return;

        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (boots.isEmpty() || !(boots.getItem() instanceof QuantumBoots)) return;
        if (!QuantumBoots.isSuperJumpEnabled(boots)) return;
        if (!QuantumBoots.consumeJumpEnergy(boots)) return;

        Vec3d vel = player.getVelocity();
        double multiplier = QuantumBoots.getJumpHeightMultiplier();
        player.setVelocity(vel.x, vel.y * multiplier, vel.z);
    }
}

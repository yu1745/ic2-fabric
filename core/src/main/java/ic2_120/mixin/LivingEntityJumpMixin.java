package ic2_120.mixin;

import ic2_120.access.SuperJumpProtectionAccess;
import ic2_120.content.item.armor.QuantumBoots;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void ic2$onJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty() || !(boots.getItem() instanceof QuantumBoots)) return;

        if (!QuantumBoots.isSuperJumpEnabled(boots)) return;

        // 能量不足时不触发大跳
        if (!QuantumBoots.consumeJumpEnergy(boots)) return;

        // 倍率跳跃高度
        Vec3 vel = player.getDeltaMovement();
        double multiplier = QuantumBoots.getJumpHeightMultiplier();
        player.setDeltaMovement(vel.x, vel.y * multiplier, vel.z);

        if (!QuantumBoots.isPermanentFallProtectionEnabled()) {
            // 常驻保护关闭时，保护状态属于本次跳跃的玩家，不能跟随靴子 NBT 持久化或转移。
            ((SuperJumpProtectionAccess) player).ic2$activateSuperJumpProtection(boots);
        }
    }
}

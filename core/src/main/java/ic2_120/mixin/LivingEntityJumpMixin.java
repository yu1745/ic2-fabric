package ic2_120.mixin;

import ic2_120.content.item.armor.QuantumBoots;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jump", at = @At("TAIL"))
    private void ic2$onJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (boots.isEmpty() || !(boots.getItem() instanceof QuantumBoots)) return;

        if (!QuantumBoots.isSuperJumpEnabled(boots)) return;

        // 能量不足时不触发大跳
        if (!QuantumBoots.consumeJumpEnergy(boots)) return;

        // 倍率跳跃高度
        Vec3d vel = player.getVelocity();
        double multiplier = QuantumBoots.getJumpHeightMultiplier();
        player.setVelocity(vel.x, vel.y * multiplier, vel.z);

        // 标记摔落保护
        QuantumBoots.markSuperJumpProtection(boots);
    }
}

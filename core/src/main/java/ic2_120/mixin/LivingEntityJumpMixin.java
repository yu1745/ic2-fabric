package ic2_120.mixin;

<<<<<<< HEAD
import ic2_120.access.SuperJumpProtectionAccess;
import ic2_120.content.item.armor.QuantumBoots;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
=======
import ic2_120.content.item.armor.QuantumBoots;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
>>>>>>> backup/1.21.1
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
<<<<<<< HEAD
        if (!(self instanceof ServerPlayerEntity player)) return;
=======
        if (!(self instanceof PlayerEntity player)) return;
>>>>>>> backup/1.21.1

        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (boots.isEmpty() || !(boots.getItem() instanceof QuantumBoots)) return;

        if (!QuantumBoots.isSuperJumpEnabled(boots)) return;

        // 能量不足时不触发大跳
        if (!QuantumBoots.consumeJumpEnergy(boots)) return;

        // 倍率跳跃高度
        Vec3d vel = player.getVelocity();
        double multiplier = QuantumBoots.getJumpHeightMultiplier();
        player.setVelocity(vel.x, vel.y * multiplier, vel.z);

<<<<<<< HEAD
        if (!QuantumBoots.isPermanentFallProtectionEnabled()) {
            // 常驻保护关闭时，保护状态属于本次跳跃的玩家，不能跟随靴子 NBT 持久化或转移。
            ((SuperJumpProtectionAccess) player).ic2$activateSuperJumpProtection(boots);
        }
=======
        // 标记摔落保护
        QuantumBoots.markSuperJumpProtection(boots);
>>>>>>> backup/1.21.1
    }
}

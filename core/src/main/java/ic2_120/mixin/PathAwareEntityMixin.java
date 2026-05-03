package ic2_120.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当生物被拴绳动能发生机的隐形盔甲架拴住时，
 * 取消拴绳的拉扯和跟随效果，允许生物自由跑动。
 * 同时保留兜底逻辑：超过 20 格自动断开拴绳。
 */
@Mixin(PathAwareEntity.class)
public class PathAwareEntityMixin {

    /**
     * 在 super.updateLeash() 之后、拉扯逻辑之前注入。
     * 如果持有者是隐形无重力盔甲架（动能发生机的锚点），则只做距离兜底检查后跳过原方法。
     */
    @Inject(
        method = "updateLeash",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/mob/MobEntity;updateLeash()V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void ic2$skipLeashPullingForKineticGenerator(CallbackInfo ci) {
        PathAwareEntity self = (PathAwareEntity) (Object) this;
        Entity holdingEntity = self.getHoldingEntity();

        if (holdingEntity instanceof ArmorStandEntity armorStand
            && armorStand.isInvisible()
            && armorStand.hasNoGravity()) {
            // 兜底：距离过远时断开拴绳
            float distance = self.distanceTo(holdingEntity);
            if (!self.isAlive() || !holdingEntity.isAlive() || distance > 20.0F) {
                self.detachLeash(true, true);
            }
            ci.cancel();
        }
    }
}

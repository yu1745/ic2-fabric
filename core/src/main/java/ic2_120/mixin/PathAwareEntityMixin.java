package ic2_120.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 当生物被拴绳动能发生机的隐形盔甲架拴住时，
 * 取消拴绳的拉扯和跟随效果，允许生物自由跑动。
 * 同时保留兜底逻辑：超过 20 格自动断开拴绳。
 *
 * 1.21.1 使用 Leashable.beforeLeashTick 返回 false 跳过默认拉扯逻辑。
 */
@Mixin(PathAwareEntity.class)
public class PathAwareEntityMixin {

    /**
     * 拴绳 tick 开始时检查：如果持有者是隐形无重力盔甲架（动能发生机的锚点），
     * 返回 false 以跳过默认的拉扯/跟随逻辑，仅保留距离兜底检查。
     */
    @Inject(method = "beforeLeashTick", at = @At("HEAD"), cancellable = true)
    private void ic2$skipLeashPullingForKineticGenerator(Entity leashHolder, float distance, CallbackInfoReturnable<Boolean> cir) {
        if (leashHolder instanceof ArmorStandEntity armorStand
            && armorStand.isInvisible()
            && armorStand.hasNoGravity()) {
            PathAwareEntity self = (PathAwareEntity) (Object) this;
            // 兜底：距离过远时断开拴绳
            if (!self.isAlive() || !leashHolder.isAlive() || distance > 20.0F) {
                self.detachLeash(true, true);
            }
            cir.setReturnValue(false);
        }
    }
}

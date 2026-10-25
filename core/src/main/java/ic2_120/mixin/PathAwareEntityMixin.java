package ic2_120.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当生物被拴绳动能发生机的隐形盔甲架拴住时，
 * 取消拴绳的拉扯和跟随效果，允许生物自由跑动。
 * 同时保留兜底逻辑：超过 20 格自动断开拴绳。
 *
 * 1.21.1 把原 Mob.updateLeash 的每 tick 拴绳逻辑合并进了
 * Leashable.tickLeash 静态方法，改为在其头部拦截。
 */
@Mixin(Leashable.class)
public interface PathAwareEntityMixin {

    @Inject(
        method = "tickLeash",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void ic2$skipLeashPullingForKineticGenerator(ServerLevel level, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof PathfinderMob self)) {
            return;
        }
        Entity holdingEntity = self.getLeashHolder();

        if (holdingEntity instanceof ArmorStand armorStand
            && armorStand.isInvisible()
            && armorStand.isNoGravity()) {
            // 兜底：距离过远时断开拴绳
            float distance = self.distanceTo(holdingEntity);
            if (!self.isAlive() || !holdingEntity.isAlive() || distance > 20.0F) {
                self.dropLeash();
            }
            ci.cancel();
        }
    }

    /**
     * 区块重新加载时的另一条路径：
     * readLeashNbt() → Leashable.setLeashedTo() → stopRiding()
     * 原版 setLeashedTo 在绑定拴绳时会强制让骑乘中的生物下车，
     * 此处拦截：当持有者是盔甲架（即动能发生机的锚点）时跳过下车。
     */
    @Redirect(
        method = "setLeashedTo(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;stopRiding()V")
    )
    private static void ic2$keepRidingOnLeashAttach(Entity entity) {
        if (((Leashable) entity).getLeashHolder() instanceof ArmorStand) {
            return;
        }
        entity.stopRiding();
    }

}

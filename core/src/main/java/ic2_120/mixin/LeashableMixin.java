package ic2_120.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 拴绳动能发生机使用隐形盔甲架作为拴绳锚点。
 * <p>
 * 原版 attachLeash 在绑定拴绳时会强制让骑乘中的生物下车（hasVehicle → stopRiding），
 * 此处拦截：当持有者是盔甲架（即动能发生机的锚点）时跳过下车。
 * <p>
 * 1.21.1 将拴绳逻辑提取到 {@link Leashable} 接口（原在 MobEntity 中），
 * 因此不能在 {@code MobEntityMixin} 中拦截 attachLeash，需要单独的混入。
 */
@Mixin(Leashable.class)
public interface LeashableMixin {

    /**
     * 拦截 {@link Leashable#attachLeash(Entity, boolean)} 中的
     * {@code entity.stopRiding()} 调用，
     * 当持有者是盔甲架时跳过下车，防止动能发生机拴住的生物从矿车掉出。
     */
    @Redirect(
        method = "attachLeash(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;stopRiding()V")
    )
    private static void keepRidingOnLeashAttach(Entity entity) {
        if (entity instanceof Leashable leashable && leashable.getLeashHolder() instanceof ArmorStandEntity) {
            return;
        }
        entity.stopRiding();
    }
}

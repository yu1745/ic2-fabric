package ic2_120.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 允许大部分怪物被拴绳拴住（原版排除了所有 Monster）。
 * 排除：凋零（Boss）、恶魂、幻翼（飞行类）。
 * 末影龙不是 MobEntity，无需额外处理。
 *
 * 同时阻止拴绳动能发生机拴住的生物在骑乘（如坐上矿车）时拴绳断裂。
 */
@Mixin(MobEntity.class)
public class MobEntityMixin {

    /**
     * @author ic2_120
     * @reason 允许拴绳动能发生机拴住怪物
     */
    @Overwrite
    public boolean canBeLeashed() {
        MobEntity self = (MobEntity) (Object) this;
        if (self instanceof WitherEntity
            || self instanceof GhastEntity
            || self instanceof PhantomEntity) {
            return false;
        }
        return !self.isLeashed();
    }

    /**
     * 拴绳动能发生机使用隐形盔甲架作为拴绳锚点。
     * 原版 startRiding 会在生物骑乘时强制断开拴绳，
     * 此处拦截：当持有者是盔甲架（即动能发生机的锚点）时跳过断开。
     */
    @Redirect(
        method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;detachLeash(ZZ)V")
    )
    private void keepLeashOnKineticGenerator(MobEntity instance, boolean sendPacket, boolean dropItem) {
        if (instance.getHoldingEntity() instanceof ArmorStandEntity) {
            return;
        }
        instance.detachLeash(sendPacket, dropItem);
    }
}

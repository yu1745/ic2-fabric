package ic2_120.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 允许大部分怪物被拴绳拴住（原版排除了所有 Enemy）。
 * 排除：凋零（Boss）、恶魂、幻翼（飞行类）。
 * 末影龙不是 Mob，无需额外处理。
 *
 * 同时阻止拴绳动能发生机拴住的生物在骑乘（如坐上矿车）时拴绳断裂。
 */
@Mixin(Mob.class)
public class MobEntityMixin {

    /**
     * @author ic2_120
     * @reason 允许拴绳动能发生机拴住怪物
     */
    @Overwrite
    public boolean canBeLeashedBy(Player player) {
        Mob self = (Mob) (Object) this;
        if (self instanceof WitherBoss
            || self instanceof Ghast
            || self instanceof Phantom) {
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
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/Mob;detachLeash(ZZ)V")
    )
    private void keepLeashOnKineticGenerator(Mob instance, boolean sendPacket, boolean dropItem) {
        if (instance.getLeashHolder() instanceof ArmorStand) {
            return;
        }
        instance.dropLeash(sendPacket, dropItem);
    }

    /**
     * 区块重新加载时的另一条路径：
     * readLeashNbt() → attachLeash() → stopRiding()
     * 原版 attachLeash 在绑定拴绳时会强制让骑乘中的生物下车，
     * 此处拦截：当持有者是盔甲架（即动能发生机的锚点）时跳过下车。
     */
    @Redirect(
        method = "attachLeash",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/Mob;stopRiding()V")
    )
    private void keepRidingOnLeashAttach(Mob instance) {
        if (instance.getLeashHolder() instanceof ArmorStand) {
            return;
        }
        instance.stopRiding();
    }
}

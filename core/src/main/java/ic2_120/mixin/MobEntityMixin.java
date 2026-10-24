package ic2_120.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
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
    public boolean canBeLeashed() {
        Mob self = (Mob) (Object) this;
        if (self instanceof WitherBoss
            || self instanceof Ghast
            || self instanceof Phantom) {
            return false;
        }
        return !self.isLeashed();
    }

    // 1.21.1 的 Entity.startRiding 不再强制断开拴绳，原 startRiding→detachLeash 拦截已不需要。


}

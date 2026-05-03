package ic2_120.mixin;

import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 允许大部分怪物被拴绳拴住（原版排除了所有 Monster）。
 * 排除：凋零（Boss）、恶魂、幻翼（飞行类）。
 * 末影龙不是 MobEntity，无需额外处理。
 */
@Mixin(MobEntity.class)
public class MobEntityMixin {

    /**
     * @author ic2_120
     * @reason 允许拴绳动能发生机拴住怪物
     */
    @Overwrite
    public boolean canBeLeashedBy(PlayerEntity player) {
        MobEntity self = (MobEntity) (Object) this;
        if (self instanceof WitherEntity
            || self instanceof GhastEntity
            || self instanceof PhantomEntity) {
            return false;
        }
        return !self.isLeashed();
    }
}

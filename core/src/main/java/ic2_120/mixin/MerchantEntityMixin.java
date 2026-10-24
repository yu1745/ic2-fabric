package ic2_120.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 允许村民被拴绳拴住，使拴绳动能发生机可以正常工作。
 */
@Mixin(AbstractVillager.class)
public class MerchantEntityMixin {

    /**
     * @author ic2_120
     * @reason 原版 AbstractVillager.canBeLeashedBy 返回 false，导致村民无法被拴绳
     */
    @Overwrite
    public boolean canBeLeashed() {
        return true;
    }
}

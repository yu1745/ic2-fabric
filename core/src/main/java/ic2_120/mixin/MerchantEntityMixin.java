package ic2_120.mixin;

import net.minecraft.entity.passive.MerchantEntity;
<<<<<<< HEAD
import net.minecraft.entity.player.PlayerEntity;
=======
>>>>>>> backup/1.21.1
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 允许村民被拴绳拴住，使拴绳动能发生机可以正常工作。
 */
@Mixin(MerchantEntity.class)
public class MerchantEntityMixin {

    /**
     * @author ic2_120
<<<<<<< HEAD
     * @reason 原版 MerchantEntity.canBeLeashedBy 返回 false，导致村民无法被拴绳
     */
    @Overwrite
    public boolean canBeLeashedBy(PlayerEntity player) {
=======
     * @reason 原版 MerchantEntity.canBeLeashed 返回 false，导致村民无法被拴绳
     */
    @Overwrite
    public boolean canBeLeashed() {
>>>>>>> backup/1.21.1
        return true;
    }
}

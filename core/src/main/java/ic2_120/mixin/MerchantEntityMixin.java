package ic2_120.mixin;

import net.minecraft.entity.passive.MerchantEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 允许村民被拴绳拴住，使拴绳动能发生机可以正常工作。
 */
@Mixin(MerchantEntity.class)
public class MerchantEntityMixin {

    /**
     * @author ic2_120
     * @reason 原版 MerchantEntity.canBeLeashed 返回 false，导致村民无法被拴绳
     */
    @Overwrite
    public boolean canBeLeashed() {
        return true;
    }
}

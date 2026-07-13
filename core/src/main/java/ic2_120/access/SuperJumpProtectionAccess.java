package ic2_120.access;

import net.minecraft.item.ItemStack;

/**
 * 玩家本次量子靴大跳的临时落地保护状态。
 */
public interface SuperJumpProtectionAccess {

    void ic2$activateSuperJumpProtection(ItemStack boots);
}

package ic2_120.mixin;

import ic2_120.content.item.armor.ElectricArmorItem;
import ic2_120.util.NanoSaberDamageHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家实体 Mixin
 *
 * 实现：
 * 1. 纳米/量子护甲减伤逻辑
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    private static final int DAMAGE_COST_PER_POINT = 5000;

    /**
     * 防止 {@link #onDamage} 在递归调用 {@link PlayerEntity#damage} 时再次套用电力护甲减伤。
     */
    private static final ThreadLocal<Boolean> IC2_ELECTRIC_ARMOR_DAMAGE_RECURSE =
            ThreadLocal.withInitial(() -> false);

    /**
     * 纳米剑对穿戴纳米套的玩家：替换为无视原版护甲的伤害类型；量子护甲时不替换（见 {@link NanoSaberDamageHelper}）。
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private DamageSource ic2ReplaceDamageSourceForNanoSaber(DamageSource source) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (NanoSaberDamageHelper.shouldReplaceWithPierce(player, source)) {
            return NanoSaberDamageHelper.createPierceDamage(player, source);
        }
        return source;
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (IC2_ELECTRIC_ARMOR_DAMAGE_RECURSE.get()) {
            return;
        }

        if (source.getName().equals("outOfWorld")) {
            return;
        }

        // 纳米剑穿甲伤害：不走纳米/量子 EU 减伤（量子套不会收到此伤害类型）
        if (NanoSaberDamageHelper.isNanoSaberArmorPierceDamage(source)) {
            return;
        }

        float totalReduction = 0f;
        Map<ElectricArmorItem, ItemStack> armorItems = new HashMap<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ElectricArmorItem) {
                ElectricArmorItem armor = (ElectricArmorItem) stack.getItem();
                long energy = armor.getEnergy(stack);
                if (energy > 0) {
                    armorItems.put(armor, stack);
                    totalReduction += armor.getDamageReduction();
                }
            }
        }

        if (totalReduction <= 0f || armorItems.isEmpty()) return;

        float reducedDamage = amount * (1f - Math.min(totalReduction, 1f));
        if (reducedDamage >= amount) return;

        double mitigated = (double) amount - (double) reducedDamage;
        if (mitigated <= 0.0) return;

        long energyNeeded = (long) Math.ceil(mitigated) * DAMAGE_COST_PER_POINT;

        long totalEnergy = 0;
        for (Map.Entry<ElectricArmorItem, ItemStack> entry : armorItems.entrySet()) {
            totalEnergy += entry.getKey().getEnergy(entry.getValue());
        }

        if (totalEnergy < energyNeeded) {
            return;
        }

        long remainingEnergy = energyNeeded;
        int itemCount = armorItems.size();
        long energyPerItem = energyNeeded / itemCount;
        long extraEnergy = energyNeeded % itemCount;

        for (Map.Entry<ElectricArmorItem, ItemStack> entry : armorItems.entrySet()) {
            ElectricArmorItem armor = entry.getKey();
            ItemStack stack = entry.getValue();
            long available = armor.getEnergy(stack);

            long toConsume = Math.min(available, energyPerItem);
            if (extraEnergy > 0 && available > energyPerItem) {
                toConsume = Math.min(available, energyPerItem + 1);
                extraEnergy--;
            }

            armor.setEnergy(stack, available - toConsume);
            remainingEnergy -= toConsume;
        }

        if (remainingEnergy > 0) {
            for (Map.Entry<ElectricArmorItem, ItemStack> entry : armorItems.entrySet()) {
                if (remainingEnergy <= 0) break;
                ElectricArmorItem armor = entry.getKey();
                ItemStack stack = entry.getValue();
                long available = armor.getEnergy(stack);

                if (available > 0) {
                    long toConsume = Math.min(available, remainingEnergy);
                    armor.setEnergy(stack, available - toConsume);
                    remainingEnergy -= toConsume;
                }
            }
        }

        IC2_ELECTRIC_ARMOR_DAMAGE_RECURSE.set(true);
        try {
            cir.setReturnValue(player.damage(source, reducedDamage));
        } finally {
            IC2_ELECTRIC_ARMOR_DAMAGE_RECURSE.set(false);
        }
        cir.cancel();
    }
}

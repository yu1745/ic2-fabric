package ic2_120.mixin;

import ic2_120.config.Ic2Config;
import ic2_120.access.SuperJumpProtectionAccess;
import ic2_120.content.item.armor.ElectricArmorItem;
import ic2_120.content.item.armor.QuantumBoots;
import ic2_120.util.NanoSaberDamageHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
public abstract class PlayerEntityMixin implements SuperJumpProtectionAccess {

    private static final long DAMAGE_COST_PER_POINT = 20000L;

    @Unique
    private static final int IC2_SUPER_JUMP_PROTECTION_MAX_TICKS = 200;

    @Unique
    private boolean ic2$superJumpProtectionActive;

    @Unique
    private boolean ic2$superJumpLeftGround;

    @Unique
    private int ic2$superJumpGroundedSinceAge = -1;

    @Unique
    private int ic2$superJumpProtectionStartAge;

    @Unique
    private ItemStack ic2$superJumpProtectionBoots = ItemStack.EMPTY;

    @Unique
    private World ic2$superJumpProtectionWorld;

    /**
     * 防止 {@link #onDamage} 在递归调用 {@link PlayerEntity#damage} 时再次套用电力护甲减伤。
     */
    private static final ThreadLocal<Boolean> IC2_ELECTRIC_ARMOR_DAMAGE_RECURSE =
            ThreadLocal.withInitial(() -> false);

    @Override
    public void ic2$activateSuperJumpProtection(ItemStack boots) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        this.ic2$superJumpProtectionActive = true;
        this.ic2$superJumpLeftGround = !player.isOnGround();
        this.ic2$superJumpGroundedSinceAge = -1;
        this.ic2$superJumpProtectionStartAge = player.age;
        this.ic2$superJumpProtectionBoots = boots;
        this.ic2$superJumpProtectionWorld = player.getWorld();
    }

    @Unique
    private void ic2$clearSuperJumpProtection() {
        this.ic2$superJumpProtectionActive = false;
        this.ic2$superJumpLeftGround = false;
        this.ic2$superJumpGroundedSinceAge = -1;
        this.ic2$superJumpProtectionStartAge = 0;
        this.ic2$superJumpProtectionBoots = ItemStack.EMPTY;
        this.ic2$superJumpProtectionWorld = null;
    }

    @Unique
    private boolean ic2$isProtectedBootsStillEquipped(PlayerEntity player) {
        ItemStack equippedBoots = player.getEquippedStack(EquipmentSlot.FEET);
        return equippedBoots == this.ic2$superJumpProtectionBoots
                && equippedBoots.getItem() instanceof QuantumBoots;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ic2$tickSuperJumpProtection(CallbackInfo ci) {
        if (!this.ic2$superJumpProtectionActive) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        int protectionAge = player.age - this.ic2$superJumpProtectionStartAge;
        if (protectionAge < 0
                || protectionAge > IC2_SUPER_JUMP_PROTECTION_MAX_TICKS
                || !player.isAlive()
                || player.getWorld() != this.ic2$superJumpProtectionWorld
                || !this.ic2$isProtectedBootsStillEquipped(player)
                || player.isTouchingWater()
                || player.isClimbing()
                || player.hasVehicle()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.hasStatusEffect(StatusEffects.LEVITATION)) {
            this.ic2$clearSuperJumpProtection();
            return;
        }

        if (!player.isOnGround()) {
            this.ic2$superJumpLeftGround = true;
            this.ic2$superJumpGroundedSinceAge = -1;
        } else if (this.ic2$superJumpLeftGround) {
            if (this.ic2$superJumpGroundedSinceAge < 0) {
                // 服务端可能先在玩家 tick 中观察到 onGround，再处理触发摔落伤害的移动包。
                this.ic2$superJumpGroundedSinceAge = player.age;
            } else if (player.age - this.ic2$superJumpGroundedSinceAge >= 2) {
                // 两 tick 内仍没有 handleFallDamage，视为无伤落地并结束本次保护。
                this.ic2$clearSuperJumpProtection();
            }
        }
    }

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void ic2$handleSuperJumpFallDamage(
            float fallDistance,
            float damageMultiplier,
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) {
            return;
        }

        if (QuantumBoots.isPermanentFallProtectionEnabled()) {
            if (this.ic2$superJumpProtectionActive) {
                this.ic2$clearSuperJumpProtection();
            }
            ItemStack equippedBoots = player.getEquippedStack(EquipmentSlot.FEET);
            if (QuantumBoots.tryAbsorbPermanentFallDamage(equippedBoots, fallDistance)) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (!this.ic2$superJumpProtectionActive) {
            return;
        }

        boolean shouldProtect = this.ic2$isProtectedBootsStillEquipped(player)
                && player.getWorld() == this.ic2$superJumpProtectionWorld;
        this.ic2$clearSuperJumpProtection();

        if (shouldProtect) {
            cir.setReturnValue(false);
        }
    }

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
                float reduction = armor.getDamageReduction();
                // 只加入实际提供减伤的护甲，避免电力喷气背包等 0 减伤装备被错误扣除电量
                if (reduction > 0f) {
                    long energy = armor.getEnergy(stack);
                    if (energy > 0) {
                        armorItems.put(armor, stack);
                        totalReduction += reduction;
                    }
                }
            }
        }

        if (totalReduction <= 0f || armorItems.isEmpty()) return;

        float reducedDamage = amount * (1f - Math.min(totalReduction, 1f));
        if (reducedDamage >= amount) return;

        double mitigated = (double) amount - (double) reducedDamage;
        if (mitigated <= 0.0) return;

        long energyNeeded = (long) Math.ceil(
            mitigated * DAMAGE_COST_PER_POINT * Ic2Config.INSTANCE.getElectricArmorDamageCostMultiplier()
        );

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

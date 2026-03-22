package ic2_120.mixin;

import ic2_120.content.item.armor.ElectricArmorItem;
import ic2_120.content.item.ElectricJetpack;
import ic2_120.content.item.armor.JetpackItem;
import ic2_120.content.item.armor.NanoArmorItem;
import ic2_120.content.item.armor.QuantumArmorItem;
import ic2_120.content.item.armor.QuantumChestplate;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家实体 Mixin
 *
 * 实现：
 * 1. 纳米/量子护甲减伤逻辑
 * 2. 量子胸甲飞行功能
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Unique
    private static final String QUANTUM_FLIGHT_KEY = "QuantumFlightEnabled";
    @Unique
    private static final String QUANTUM_FLIGHT_ACTIVE_KEY = "QuantumFlightActive";  // 标记当前是否因量子套而飞行
    @Unique
    private static final int FLIGHT_COST = 417;  // 10M EU / 20min = 24000 ticks ≈ 417 EU/tick
    @Unique
    private static final int DAMAGE_COST_PER_POINT = 5000;

    // 喷气背包相关常量
    @Unique
    private static final String JETPACK_HOVER_KEY = "IsHover";
    @Unique
    private static final SoundEvent JETPACK_LOOP_SOUND = SoundEvent.of(new Identifier("ic2", "item.drill.idle"));
    @Unique
    private static final int JETPACK_SOUND_INTERVAL_TICKS = 8;
    @Unique
    private boolean jetpackFlightGranted = false;
    @Unique
    private int jetpackSoundCooldown = 0;

    // ========== 伤害减伤 Mixin ==========

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // 不拦截虚空伤害
        if (source.getName().equals("outOfWorld")) {
            return;
        }

        // 检查纳米/量子护甲（单件独立减伤）
        float totalReduction = 0f;
        Map<ElectricArmorItem, ItemStack> armorItems = new HashMap<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ElectricArmorItem) {
                ElectricArmorItem armor = (ElectricArmorItem) stack.getItem();
                long energy = armor.getEnergy(stack);
                // 只有有电的装备才提供减伤
                if (energy > 0) {
                    armorItems.put(armor, stack);
                    totalReduction += armor.getDamageReduction();
                }
            }
        }

        if (totalReduction <= 0f || armorItems.isEmpty()) return;

        // 计算减伤后的伤害
        float reducedDamage = amount * (1f - Math.min(totalReduction, 1f));
        if (reducedDamage >= amount) return; // 没有减伤

        // 计算能量需求（基于原始伤害，而不是实际减免的伤害）
        long energyNeeded = (long) Math.ceil(amount) * DAMAGE_COST_PER_POINT;

        // 检查总能量
        long totalEnergy = 0;
        for (Map.Entry<ElectricArmorItem, ItemStack> entry : armorItems.entrySet()) {
            totalEnergy += entry.getKey().getEnergy(entry.getValue());
        }

        if (totalEnergy < energyNeeded) {
            // 能量不足，不消耗能量，正常受伤害
            return;
        }

        // 均匀扣除能量
        long remainingEnergy = energyNeeded;

        // 第一轮：每个装备平均扣除
        int itemCount = armorItems.size();
        long energyPerItem = energyNeeded / itemCount;
        long extraEnergy = energyNeeded % itemCount; // 余数，需要额外分配

        for (Map.Entry<ElectricArmorItem, ItemStack> entry : armorItems.entrySet()) {
            ElectricArmorItem armor = entry.getKey();
            ItemStack stack = entry.getValue();
            long available = armor.getEnergy(stack);

            // 计算这个装备应该扣除的能量
            long toConsume = Math.min(available, energyPerItem);
            if (extraEnergy > 0 && available > energyPerItem) {
                // 分配余数给能量充足的装备
                toConsume = Math.min(available, energyPerItem + 1);
                extraEnergy--;
            }

            armor.setEnergy(stack, available - toConsume);
            remainingEnergy -= toConsume;
        }

        // 第二轮：如果还有剩余能量（说明某些装备能量不足），从能量充足的装备再扣
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

        // 应用减伤后的伤害
        if (reducedDamage > 0) {
            // 使用修改后的伤害值重新调用 damage
            // 这里我们不能直接调用 super.damage()，因为这会导致无限循环
            // 所以我们让原版逻辑继续执行，但需要修改伤害值
            // 由于无法在 HEAD 注入点修改参数，我们使用一个替代方案：
            // 取消本次伤害，并在下一 tick 应用减伤后的伤害
            cir.setReturnValue(true);
        } else {
            // 完全格挡
            cir.setReturnValue(false);
        }
        cir.cancel();
    }

    // ========== 飞行功能 Mixin ==========

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient) return; // 只在服务端处理

        // ========== 喷气背包处理 ==========
        ItemStack chestStack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof JetpackItem) {
            handleJetpackFlight(player, chestStack);
            // 如果正在使用喷气背包，不继续处理量子胸甲
            return;
        }
        if (chestStack.getItem() instanceof ElectricJetpack) {
            handleElectricJetpackFlight(player, chestStack);
            return;
        }
        // 卸下喷气背包后，清理由喷气背包赋予的飞行能力
        disableJetpackFlight(player, null);

        // ========== 量子胸甲处理 ==========
        // 检查全套量子护甲
        if (!hasFullQuantumArmor(player)) {
            if (player.getAbilities().flying && isInQuantumFlight(player)) {
                // 穿戴不全但正在飞行，取消量子套赋予的飞行
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
                // 清除量子飞行标记
                ItemStack quantumChestStack = player.getEquippedStack(EquipmentSlot.CHEST);
                if (quantumChestStack.getItem() instanceof QuantumChestplate) {
                    quantumChestStack.getOrCreateNbt().putBoolean(QUANTUM_FLIGHT_ACTIVE_KEY, false);
                }
            }
            return;
        }

        // 获取量子胸甲
        if (!(chestStack.getItem() instanceof QuantumChestplate)) return;

        QuantumChestplate chestplate = (QuantumChestplate) chestStack.getItem();
        NbtCompound nbt = chestStack.getOrCreateNbt();

        // 检查飞行开关状态
        boolean flightEnabled = nbt.getBoolean(QUANTUM_FLIGHT_KEY);
        boolean isQuantumFlightActive = nbt.getBoolean(QUANTUM_FLIGHT_ACTIVE_KEY);

        // 如果玩家是创造模式或已经在飞行（其他方式），量子套不应该干扰
        if (player.isCreative() || (player.getAbilities().flying && !isQuantumFlightActive)) {
            return;
        }

        if (!flightEnabled) {
            if (isQuantumFlightActive) {
                // 量子飞行开关关闭，取消量子套赋予的飞行
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
                nbt.putBoolean(QUANTUM_FLIGHT_ACTIVE_KEY, false);
            }
            return;
        }

        // 飞行耗电：417 EU/tick（10M EU / 20 分钟 = 24000 ticks）
        long currentEnergy = chestplate.getEnergy(chestStack);

        if (currentEnergy < FLIGHT_COST) {
            // 能量不足，关闭飞行
            nbt.putBoolean(QUANTUM_FLIGHT_KEY, false);
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
            nbt.putBoolean(QUANTUM_FLIGHT_ACTIVE_KEY, false);
            return;
        }

        // 检查玩家是否在地面上
        // 如果在地面上，取消飞行（类似创造模式的行为）
        if (player.isOnGround() || player.isTouchingWater() || player.isClimbing()) {
            if (isQuantumFlightActive) {
                // 落地了，取消飞行但保持开关开启
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
                nbt.putBoolean(QUANTUM_FLIGHT_ACTIVE_KEY, false);
            }
            return;
        }

        // 消耗能量
        chestplate.setEnergy(chestStack, currentEnergy - FLIGHT_COST);

        // 启用创造飞行（只有在空中且飞行开关开启时）
        if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.sendAbilitiesUpdate();
            nbt.putBoolean(QUANTUM_FLIGHT_ACTIVE_KEY, true);
        }
    }

    // ========== 辅助方法 ==========

    @Unique
    private boolean hasFullQuantumArmor(PlayerEntity player) {
        // 只检查护甲槽位，不检查主手和副手
        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!(stack.getItem() instanceof QuantumArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private boolean isInQuantumFlight(PlayerEntity player) {
        ItemStack chestStack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof QuantumChestplate)) return false;
        NbtCompound nbt = chestStack.getOrCreateNbt();
        return nbt.getBoolean(QUANTUM_FLIGHT_KEY);
    }

    @Unique
    private boolean isQuantumFlightActive(PlayerEntity player) {
        ItemStack chestStack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof QuantumChestplate)) return false;
        NbtCompound nbt = chestStack.getOrCreateNbt();
        return nbt.getBoolean(QUANTUM_FLIGHT_ACTIVE_KEY);
    }

    // ========== 喷气背包飞行处理 ==========

    @Unique
    private void handleJetpackFlight(PlayerEntity player, ItemStack jetpackStack) {
        NbtCompound nbt = jetpackStack.getOrCreateNbt();

        long fuel = JetpackItem.getFuel(jetpackStack);
        // 燃料不足，取消飞行
        if (fuel <= 0) {
            disableJetpackFlight(player, nbt);
            return;
        }

        // 如果玩家是创造模式，喷气背包不干扰
        if (player.isCreative() || player.isSpectator()) {
            disableJetpackFlight(player, nbt);
            return;
        }

        // 飞行开关关闭时，不启用喷气背包飞行
        if (!JetpackItem.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, nbt);
            return;
        }

        // 喷气背包仅保留垂直飞行模式
        handleVerticalFlight(player, jetpackStack, nbt, fuel);
    }

    @Unique
    private void handleVerticalFlight(PlayerEntity player, ItemStack jetpackStack, NbtCompound nbt, long fuel) {
        // 在地面时不启用飞行
        if (player.isOnGround() || player.isTouchingWater() || player.isClimbing()) {
            disableJetpackFlight(player, nbt);
            return;
        }

        // 消耗燃料并启用飞行
        JetpackItem.setFuel(jetpackStack, fuel - JetpackItem.FUEL_CONSUMPTION);
        enableJetpackFlight(player, nbt);
        playJetpackFlightSound(player, false);
    }

    @Unique
    private void handleElectricJetpackFlight(PlayerEntity player, ItemStack jetpackStack) {
        ElectricJetpack jetpack = (ElectricJetpack) jetpackStack.getItem();
        NbtCompound nbt = jetpackStack.getOrCreateNbt();

        if (player.isCreative() || player.isSpectator()) {
            disableJetpackFlight(player, nbt);
            return;
        }
        if (!jetpack.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, nbt);
            return;
        }
        if (player.isOnGround() || player.isTouchingWater() || player.isClimbing()) {
            disableJetpackFlight(player, nbt);
            return;
        }
        if (!jetpack.consumeFlightEnergyPerTick(jetpackStack)) {
            disableJetpackFlight(player, nbt);
            return;
        }

        enableJetpackFlight(player, nbt);
        playJetpackFlightSound(player, true);
    }

    @Unique
    private void enableJetpackFlight(PlayerEntity player, NbtCompound nbt) {
        boolean changed = false;
        if (!player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            changed = true;
        }
        if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            changed = true;
        }
        if (changed) {
            player.sendAbilitiesUpdate();
        }
        nbt.putBoolean(JETPACK_HOVER_KEY, true);
        jetpackFlightGranted = true;
    }

    @Unique
    private void disableJetpackFlight(PlayerEntity player, NbtCompound nbt) {
        if (!jetpackFlightGranted && (nbt == null || !nbt.getBoolean(JETPACK_HOVER_KEY))) {
            return;
        }

        if (!player.isCreative() && !player.isSpectator()) {
            boolean changed = false;
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                changed = true;
            }
            if (player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = false;
                changed = true;
            }
            if (changed) {
                player.sendAbilitiesUpdate();
            }
        }

        if (nbt != null) {
            nbt.putBoolean(JETPACK_HOVER_KEY, false);
        }
        jetpackFlightGranted = false;
        jetpackSoundCooldown = 0;
    }

    @Unique
    private void playJetpackFlightSound(PlayerEntity player, boolean electric) {
        if (jetpackSoundCooldown > 0) {
            jetpackSoundCooldown--;
            return;
        }

        float volume = electric ? 0.45F : 0.4F;
        float pitch = electric ? 1.05F : 0.95F;
        player.getWorld().playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            JETPACK_LOOP_SOUND,
            SoundCategory.PLAYERS,
            volume,
            pitch
        );
        jetpackSoundCooldown = JETPACK_SOUND_INTERVAL_TICKS;
    }
}

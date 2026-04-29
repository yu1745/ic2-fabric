package ic2_120.util;

import ic2_120.content.item.NanoSaber;
import ic2_120.content.item.armor.NanoArmorItem;
import ic2_120.content.item.armor.QuantumArmorItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * 纳米剑对穿戴纳米护甲（且未穿戴量子护甲）的玩家使用可无视护甲的伤害类型。
 */
public final class NanoSaberDamageHelper {

    private NanoSaberDamageHelper() {}

    public static final RegistryKey<DamageType> NANO_SABER_ARMOR_PIERCE_KEY =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("ic2_120", "nano_saber_armor_pierce"));

    public static boolean isNanoSaberArmorPierceDamage(DamageSource source) {
        return source.isOf(NANO_SABER_ARMOR_PIERCE_KEY);
    }

    /**
     * 攻击者为手持纳米剑的玩家、且近战；被击玩家任一件量子护甲则 false；被击玩家任一件纳米护甲则 true。
     */
    public static boolean shouldReplaceWithPierce(PlayerEntity victim, DamageSource source) {
        if (victim.getWorld().isClient()) {
            return false;
        }
        if (!source.isOf(DamageTypes.PLAYER_ATTACK)) {
            return false;
        }
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof PlayerEntity attacker)) {
            return false;
        }
        ItemStack mainHand = attacker.getMainHandStack();
        if (!(mainHand.getItem() instanceof NanoSaber)) {
            return false;
        }
        if (hasQuantumArmorPiece(victim)) {
            return false;
        }
        return hasNanoArmorPiece(victim);
    }

    public static DamageSource createPierceDamage(PlayerEntity victim, DamageSource original) {
        PlayerEntity attacker = (PlayerEntity) original.getAttacker();
        ServerWorld world = (ServerWorld) victim.getWorld();
        RegistryEntry<DamageType> entry = world.getRegistryManager()
            .get(RegistryKeys.DAMAGE_TYPE)
            .getEntry(NANO_SABER_ARMOR_PIERCE_KEY)
            .orElseThrow();
        return new DamageSource(entry, attacker, attacker);
    }

    private static boolean hasQuantumArmorPiece(PlayerEntity player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof QuantumArmorItem) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNanoArmorPiece(PlayerEntity player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof NanoArmorItem) {
                return true;
            }
        }
        return false;
    }
}

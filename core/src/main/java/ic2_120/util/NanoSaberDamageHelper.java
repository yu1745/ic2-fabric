package ic2_120.util;

import ic2_120.content.item.NanoSaber;
import ic2_120.content.item.armor.NanoArmorItem;
import ic2_120.content.item.armor.QuantumArmorItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;

/**
 * 纳米剑对穿戴纳米护甲（且未穿戴量子护甲）的玩家使用可无视护甲的伤害类型。
 */
public final class NanoSaberDamageHelper {

    private NanoSaberDamageHelper() {}

    public static final ResourceKey<DamageType> NANO_SABER_ARMOR_PIERCE_KEY =
        ResourceKey.of(Registries.DAMAGE_TYPE, ResourceLocation.of("ic2_120", "nano_saber_armor_pierce"));

    public static boolean isNanoSaberArmorPierceDamage(DamageSource source) {
        return source.isOf(NANO_SABER_ARMOR_PIERCE_KEY);
    }

    /**
     * 攻击者为手持纳米剑的玩家、且近战；被击玩家任一件量子护甲则 false；被击玩家任一件纳米护甲则 true。
     */
    public static boolean shouldReplaceWithPierce(Player victim, DamageSource source) {
        if (victim.getWorld().isClient()) {
            return false;
        }
        if (!source.isOf(DamageTypes.PLAYER_ATTACK)) {
            return false;
        }
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof Player attacker)) {
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

    public static DamageSource createPierceDamage(Player victim, DamageSource original) {
        Player attacker = (Player) original.getAttacker();
        ServerLevel world = (ServerLevel) victim.getWorld();
        Holder<DamageType> entry = world.getRegistryManager()
            .get(Registries.DAMAGE_TYPE)
            .getEntry(NANO_SABER_ARMOR_PIERCE_KEY)
            .orElseThrow();
        return new DamageSource(entry, attacker, attacker);
    }

    private static boolean hasQuantumArmorPiece(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof QuantumArmorItem) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNanoArmorPiece(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof NanoArmorItem) {
                return true;
            }
        }
        return false;
    }
}

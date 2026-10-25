package ic2_120.compat

import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import java.util.UUID

/** Preserve the vanilla four-int UUID NBT representation after CompoundTag removed its helpers. */
fun CompoundTag.hasUUID(key: String): Boolean = getIntArray(key).map { it.size == 4 }.orElse(false)

fun CompoundTag.getUUID(key: String): UUID =
    UUIDUtil.uuidFromIntArray(getIntArray(key).filter { it.size == 4 }.orElseThrow())

fun CompoundTag.putUUID(key: String, value: UUID) {
    putIntArray(key, UUIDUtil.uuidToIntArray(value))
}

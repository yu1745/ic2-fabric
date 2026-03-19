package ic2_120.content.block.storage

import net.minecraft.util.Identifier

/**
 * 储电盒配置。四个等级（BatBox/CESU/MFE/MFSU）共用此配置结构。
 */
data class EnergyStorageConfig(
    val tier: Int,
    val capacity: Long,
    val slotCount: Int,
    val useEquipmentSlots: Boolean
) {
    companion object {
        val BATBOX = EnergyStorageConfig(
            tier = 1,
            capacity = 40_000L,
            slotCount = 1,
            useEquipmentSlots = false
        )
        val CESU = EnergyStorageConfig(
            tier = 2,
            capacity = 300_000L,
            slotCount = 1,
            useEquipmentSlots = false
        )
        val MFE = EnergyStorageConfig(
            tier = 3,
            capacity = 4_000_000L,
            slotCount = 5,
            useEquipmentSlots = true
        )
        val MFSU = EnergyStorageConfig(
            tier = 4,
            capacity = 40_000_000L,
            slotCount = 5,
            useEquipmentSlots = true
        )

        private val BY_PATH = mapOf(
            "batbox" to BATBOX,
            "cesu" to CESU,
            "mfe" to MFE,
            "mfsu" to MFSU
        )

        fun fromBlockPath(path: String): EnergyStorageConfig? = BY_PATH[path]
    }
}

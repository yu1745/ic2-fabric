package ic2_120.content.block.storage

import net.minecraft.util.Identifier

/**
 * 储电盒配置。四个等级（BatBox/CESU/MFE/MFSU）共用此配置结构。
 */
data class EnergyStorageConfig(
    val tier: Int,
    val capacity: Long,
    val slotCount: Int,
    val useEquipmentSlots: Boolean,
    val chargePlayersAbove: Boolean = false,
    /** 未满电时向周围输出红石信号（对齐原版 IC2 储电盒红石模式 “emitted when not full”）。 */
    val emitRedstoneWhenNotFull: Boolean = false
) {
    companion object {
        val BATBOX = EnergyStorageConfig(
            tier = 1,
            capacity = 40_000L,
            slotCount = 2,
            useEquipmentSlots = false,
            emitRedstoneWhenNotFull = true
        )
        val CESU = EnergyStorageConfig(
            tier = 2,
            capacity = 300_000L,
            slotCount = 2,
            useEquipmentSlots = false,
            emitRedstoneWhenNotFull = true
        )
        val MFE = EnergyStorageConfig(
            tier = 3,
            capacity = 4_000_000L,
            slotCount = 2,
            useEquipmentSlots = true,
            emitRedstoneWhenNotFull = true
        )
        val MFSU = EnergyStorageConfig(
            tier = 4,
            capacity = 40_000_000L,
            slotCount = 2,
            useEquipmentSlots = true,
            emitRedstoneWhenNotFull = true
        )
        // 充电座同样支持红石模式系统（与储电箱同源，默认 0 = 不输出）
        val BATBOX_CHARGEPAD = BATBOX.copy(chargePlayersAbove = true)
        val CESU_CHARGEPAD = CESU.copy(chargePlayersAbove = true)
        val MFE_CHARGEPAD = MFE.copy(chargePlayersAbove = true)
        val MFSU_CHARGEPAD = MFSU.copy(chargePlayersAbove = true)

        private val BY_PATH = mapOf(
            "batbox" to BATBOX,
            "cesu" to CESU,
            "mfe" to MFE,
            "mfsu" to MFSU,
            "batbox_chargepad" to BATBOX_CHARGEPAD,
            "cesu_chargepad" to CESU_CHARGEPAD,
            "mfe_chargepad" to MFE_CHARGEPAD,
            "mfsu_chargepad" to MFSU_CHARGEPAD
        )

        fun fromBlockPath(path: String): EnergyStorageConfig? = BY_PATH[path]
    }
}

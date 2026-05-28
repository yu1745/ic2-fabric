package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants

/**
 * 蒸汽发生器同步数据。
 * 不含 EU 能量容器（蒸汽发生器不直接使用 EU），仅同步温度和流体状态。
 */
class SteamGeneratorSync(
    schema: SyncSchema
) {
    companion object {
        /** 水罐容量 (droplets, 10 BUCKET) */
        val WATER_TANK_CAPACITY: Long = FluidConstants.BUCKET * 10
        /** 蒸汽罐容量 (mB) */
        const val STEAM_TANK_CAPACITY = 100_000L
        /** 最大热输入 (HU/t)，参考 ic2_origin */
        const val MAX_HEAT_INPUT = 1200L
        /** 最大系统温度 (°C * 1000，即 milli-degree) */
        const val MAX_SYSTEM_HEAT_MILLI = 500_000
        /** 过热蒸汽阈值 (°C * 1000)，374°C */
        const val SUPERHEATED_THRESHOLD_MILLI = 374_000
        /** 蒸汽膨胀倍率: 1 mB 水 → 100 mB 蒸汽 */
        const val STEAM_EXPANSION = 100
        /** 水垢上限 (累计处理水量 mB) */
        const val MAX_CALCIFICATION = 100_000L
        /** 压力上限 */
        const val MAX_PRESSURE = 300
    }

    /** 系统温度 (milli-°C, 例如 374000 = 374.0°C) */
    var systemHeatMilli by schema.int("SystemHeatMilli")

    /** 进水速率 (mB/t) */
    var inputMB by schema.int("InputMB", default = 100)

    /** 蒸汽输出速率 (mB/t) */
    var outputMB by schema.int("OutputMB")

    /** 当前压力 (bar) */
    var pressure by schema.int("Pressure", default = 100)

    /** 热输入速率 (HU/t) */
    var heatInput by schema.int("HeatInput")

    /** 水罐存水量 (mB) */
    var waterAmount by schema.int("WaterAmount")

    /** 蒸汽罐存量 (mB) */
    var steamAmount by schema.int("SteamAmount")

    /** 水垢累积量 (累计处理水量 mB) */
    var calcification by schema.int("Calcification")

    /** 当前产出是否为过热蒸汽 */
    var isSuperheated by schema.int("IsSuperheated")

    /** 水垢是否已满（机器停摆） */
    var calcified by schema.int("Calcified")

    /** 当前水罐中的水是否为蒸馏水（客户端 tooltip 显示用） */
    var isWaterDistilled by schema.int("IsWaterDistilled")
}

package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 蒸汽动能发电机（蒸汽轮机）同步数据。
 * 将蒸汽转化为动能 (KU)，通过 IKineticMachinePort 输出。
 *
 * 参考 ic2_origin 数值：
 * - 普通蒸汽: 2 KU/mB
 * - 过热蒸汽: 4 KU/mB
 * - 蒸汽罐容量: 21,000 mB
 * - 蒸馏水罐容量: 1,000 mB
 */
class SteamKineticGeneratorSync(
    schema: SyncSchema
) {
    companion object {
        /** 蒸汽罐容量 (mB) */
        const val STEAM_TANK_CAPACITY = 21_000L
        /** 蒸馏水罐容量 (mB) */
        const val DISTILLED_WATER_TANK_CAPACITY = 1_000L
        /** 普通蒸汽 → KU 转换率 (KU/mB) */
        const val KU_PER_MB_NORMAL = 2
        /** 过热蒸汽 → KU 转换率 (KU/mB) */
        const val KU_PER_MB_SUPERHEATED = 4
        /** 冷凝进度上限 (每 100 进度产出 1 mB 蒸馏水) */
        const val CONDENSATION_MAX = 100
        /** 每 tick 最大 KU 输出 */
        const val MAX_KU_OUTPUT = 2048
        /** 每 tick 最大蒸汽消耗 (mB) */
        const val MAX_STEAM_CONSUME = 100
        /** 涡轮耐久损耗 (每 tick 运行) */
        const val TURBINE_WEAR_NORMAL = 2
        const val TURBINE_WEAR_SUPERHEATED = 1
    }

    /** 蒸汽罐存量 (mB) */
    var steamAmount by schema.int("SteamAmount")

    /** 蒸馏水罐存量 (mB) */
    var distilledWaterAmount by schema.int("DistilledWaterAmount")

    /** 当前 KU 输出速率 (KU/t) */
    var kuOutput by schema.int("KuOutput")

    /** 蒸汽消耗速率 (mB/t) */
    var steamConsume by schema.int("SteamConsume")

    /** 蒸馏水产出速率 (mB/t) */
    var waterOutput by schema.int("WaterOutput")

    /** 是否为过热蒸汽模式 */
    var isSuperheated by schema.int("IsSuperheated")

    /** 冷凝进度 (0-CONDENSATION_MAX) */
    var condensationProgress by schema.int("CondensationProgress")

    /** 涡轮是否被水阻塞 (蒸馏水满) */
    var waterBlocked by schema.int("WaterBlocked")

    /** 是否有涡轮 */
    var hasTurbine by schema.int("HasTurbine")
}

package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 铁炉的同步属性。
 * 铁炉使用燃料燃烧来烧制物品，比原版熔炉快 20%。
 */
class IronFurnaceSync(
    schema: SyncSchema
) : SyncSchema by schema {

    companion object {
        const val NBT_BURN_TIME = "BurnTime"
        const val NBT_TOTAL_BURN_TIME = "TotalBurnTime"
        const val NBT_COOK_TIME = "CookTime"

        /** 烧制进度最大值 */
        const val COOK_TIME_MAX = 160  // 8秒，比原版熔炉快20%（原版200 tick）

        /** 燃料燃烧进度条最大值（用于GUI显示） */
        const val BURN_TIME_MAX = 100
    }

    /** 当前燃料剩余燃烧时间（tick） */
    var burnTime by schema.int(NBT_BURN_TIME)

    /** 当前燃料总燃烧时间（tick），用于GUI进度条 */
    var totalBurnTime by schema.int(NBT_TOTAL_BURN_TIME)

    /** 当前烧制进度（tick） */
    var cookTime by schema.int(NBT_COOK_TIME)

    /** 累积经验值 × 10（用于 GUI 显示，整数部分+一位小数） */
    var experienceDisplay by schema.int("Experience")
}

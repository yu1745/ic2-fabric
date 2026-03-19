package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 高炉同步数据。
 *
 * 规格：
 * - 预热：50,000 HU 填满
 * - 无空气缓存：每次直接从空气槽消耗 1 瓶压缩空气（每 1000 ticks 消耗 1 瓶，共 6 瓶/钢锭）
 * - 每钢锭：6 瓶压缩空气、1 铁质材料
 * - 加工时间：5 分钟（300 秒 = 6000 ticks）
 */
class BlastFurnaceSync(schema: SyncSchema) {

    companion object {
        const val PREHEAT_MAX = 50_000
        const val AIR_CELLS_PER_STEEL = 6
        const val TICKS_PER_AIR_CELL = 1_000  // 每 1000 ticks 消耗 1 瓶
        const val PROGRESS_MAX = 6_000  // 300 秒
        const val PREHEAT_REGRESS_PER_TICK = 50L  // 无热量时预热条衰减
    }

    var preheat by schema.int("Preheat", default = 0)
    var progress by schema.int("Progress", default = 0)
}

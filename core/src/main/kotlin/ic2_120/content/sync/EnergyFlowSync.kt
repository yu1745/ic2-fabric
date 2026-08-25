package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 输入/输出速率同步的复用组件。
 *
 * - 输入速率：默认取外部输入；若 [useGeneratedAsInput] 为 true，则取内部发电量。
 * - 输出速率：取外部输出。
 * - 使用滑动窗口平均，降低抖动。
 *
 * 这里同步的是“上一完整 tick”的稳定快照，避免受机器 tick / 电网 tick 执行顺序影响。
 */
class EnergyFlowSync(
    schema: SyncSchema,
    private val source: TickLimitedSidedEnergyContainer,
    private val useGeneratedAsInput: Boolean = false,
    insertedKey: String = "AvgInserted",
    extractedKey: String = "AvgExtract",
    consumedKey: String = "AvgConsume",
    windowSize: Int = 20
) {

    private var avgInsertedAmount by schema.intAveraged(insertedKey, windowSize = windowSize)
    private var avgExtractedAmount by schema.intAveraged(extractedKey, windowSize = windowSize)
    private var avgConsumedAmount by schema.intAveraged(consumedKey, windowSize = windowSize)

    fun syncCurrentTickFlow() {
        source.finalizeFlowSnapshot()

        val inserted = if (useGeneratedAsInput) {
            source.getLastGeneratedAmount()
        } else {
            source.getLastInsertedAmount()
        }
        val extracted = source.getLastExtractedAmount()
        val consumed = source.getLastConsumedAmount()

        avgInsertedAmount = inserted.toInt()
        avgExtractedAmount = extracted.toInt()
        avgConsumedAmount = consumed.toInt()

    }

    fun getSyncedInsertedAmount(): Long = avgInsertedAmount.toLong()

    fun getSyncedExtractedAmount(): Long = avgExtractedAmount.toLong()

    fun getSyncedConsumedAmount(): Long = avgConsumedAmount.toLong()
}

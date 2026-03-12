package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import org.slf4j.LoggerFactory

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

    companion object {
        private val logger = LoggerFactory.getLogger(EnergyFlowSync::class.java)
    }

    private val label = source.javaClass.simpleName
    private var lastLoggedInserted = Long.MIN_VALUE
    private var lastLoggedExtracted = Long.MIN_VALUE
    private var lastLoggedGenerated = Long.MIN_VALUE
    private var lastLoggedConsumed = Long.MIN_VALUE

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
        val generated = source.getLastGeneratedAmount()
        val consumed = source.getLastConsumedAmount()

        avgInsertedAmount = inserted.toInt()
        avgExtractedAmount = extracted.toInt()
        avgConsumedAmount = consumed.toInt()

        val changed = inserted != lastLoggedInserted || extracted != lastLoggedExtracted ||
            generated != lastLoggedGenerated || consumed != lastLoggedConsumed
        if (changed && (inserted != 0L || extracted != 0L || generated != 0L || consumed != 0L)) {
            lastLoggedInserted = inserted
            lastLoggedExtracted = extracted
            lastLoggedGenerated = generated
            lastLoggedConsumed = consumed
            logger.info(
                "EnergyFlow[{}] tick: ins={} ext={} gen={} con={} avgIns={} avgExt={}",
                label,
                inserted,
                extracted,
                generated,
                consumed,
                avgInsertedAmount,
                avgExtractedAmount
            )
        }
    }

    fun getSyncedInsertedAmount(): Long = avgInsertedAmount.toLong()

    fun getSyncedExtractedAmount(): Long = avgExtractedAmount.toLong()

    fun getSyncedConsumedAmount(): Long = avgConsumedAmount.toLong()
}

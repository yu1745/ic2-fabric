package ic2_120.content.uu

import net.minecraft.world.item.Item
import net.minecraft.core.registries.BuiltInRegistries
import org.slf4j.LoggerFactory
import java.util.IdentityHashMap
import kotlin.math.abs

/**
 * UU 物质成本图：在「物品 + 配方」构成的图上做最短路（最小成本传播），
 * 移植自 IC2 experimental 1.12.2 的 `ic2.core.uu.UuGraph` / `UuIndex`。
 *
 * ## 模型
 * - [LeanStack]：一个物品引用 + 数量（不含 NBT，按 Item 去重）
 * - [Transformation]：一条配方：多组输入（外层 AND，内层 OR 备选）→ 多个输出，
 *   [Transformation.transformCost] 为加工损耗系数（工作台 1.0、机器/熔炉 14.0、手工 0.0），
 *   与真实 EU 消耗无关
 * - 叶节点：[UuScanValues]，作为成本传播的起点
 *
 * ## 算法（Bellman-Ford 式迭代松弛，比 exp 的递归 setValue 更稳健不爆栈）
 *
 * 每轮迭代：
 *   1. 对每条配方，按当前输入值算出「输入总成本」（每组取最便宜备选）
 *      `inputCost = transformCost + Σ_groups min(备选 value × 数量)`
 *   2. 平摊到每个输出：`perItem = inputCost / outputCount`
 *   3. 若 perItem < 当前 output.value，更新
 *   4. 重复直到一轮无更新（收敛）
 *
 * 由于成本单调不增且有下界（≥ 0），必然收敛。
 *
 * ## 单位换算
 * 内部 value 与 [UuScanValues] 同单位（cobblestone = 1.0）。
 * exp 换算：`getInBuckets = value × 1e-5`；1 bucket = 1e6 uB（micro-bucket）。
 * 故 **uB = value × 1e1**，即 `costUb = (value * 10).toLong()`。
 */
object UuGraph {

    private val logger = LoggerFactory.getLogger("ic2_120.uu")
    private const val EPSILON = 1e-9
    private const val VALUE_TO_UB = 1e1  // value × 10 = uB

    /** 加工损耗系数（与 exp 对齐：工作台 1、机器 14、熔炉 14、手工 0） */
    const val CRAFTING_COST = 1.0
    const val MACHINE_COST = 14.0
    const val SMELTING_COST = 14.0
    const val MANUAL_COST = 0.0

    data class LeanStack(val item: Item, val count: Int = 1)

    data class Transformation(
        val transformCost: Double,
        /** 外层：每个 List 是一个「输入槽位」（AND），内层是该槽位的备选（OR，如标签） */
        val inputs: List<List<LeanStack>>,
        /** 输出：每个元素带自己的 count（多输出配方） */
        val outputs: List<LeanStack>
    )

    private val nodes: IdentityHashMap<Item, Double> = IdentityHashMap()  // item -> min value
    /** 锁定的物品（显式定价）：solve 时不会被动态计算覆盖，作为硬约束 */
    private val locked: IdentityHashMap<Item, Double> = IdentityHashMap()
    private val initialValues: MutableList<Pair<Item, Double>> = mutableListOf()
    private val transformations: MutableList<Transformation> = mutableListOf()

    fun setInitial(item: Item, value: Double) {
        initialValues.add(item to value)
    }

    fun addTransformation(t: Transformation) {
        transformations.add(t)
    }

    fun clear() {
        nodes.clear()
        locked.clear()
        initialValues.clear()
        transformations.clear()
    }

    /**
     * 迭代求解。返回迭代轮数（用于诊断是否在 [maxIter] 内收敛）。
     *
     * 显式定价（[setInitial] 注入的）作为**硬约束**：先写入 [locked] 和 [nodes]，
     * 松弛时跳过 locked 物品（不会被动态计算覆盖）。这样白名单定价保持权威性，
     * 动态计算只负责填充白名单未覆盖的物品。
     */
    fun solve(maxIter: Int = 2000): Int {
        nodes.clear()
        locked.clear()
        for ((item, v) in initialValues) {
            nodes[item] = v
            locked[item] = v
        }

        var iter = 0
        while (iter < maxIter) {
            var changed = false
            for (t in transformations) {
                var inputCost = t.transformCost
                var valid = true
                for (group in t.inputs) {
                    var groupMin = Double.POSITIVE_INFINITY
                    for (inp in group) {
                        val v = nodes[inp.item] ?: Double.POSITIVE_INFINITY
                        val total = v * inp.count
                        if (total < groupMin) groupMin = total
                    }
                    if (groupMin.isInfinite()) {
                        valid = false
                        break
                    }
                    inputCost += groupMin
                }
                if (!valid) continue

                for (out in t.outputs) {
                    if (out.count <= 0) continue
                    val perItem = inputCost / out.count
                    val current = nodes[out.item] ?: Double.POSITIVE_INFINITY
                    // 跳过锁定的物品（显式定价不可被覆盖）
                    if (out.item in locked) continue
                    if (perItem < current - EPSILON) {
                        nodes[out.item] = perItem
                        changed = true
                    }
                }
            }
            iter++
            if (!changed) break
        }
        return iter
    }

    /** 内部 value（cobblestone = 1.0）。+∞ 表示算不出来（无路径到叶节点）。 */
    fun value(item: Item): Double = nodes[item] ?: Double.POSITIVE_INFINITY

    /** 换算成 uB（micro-bucket）；+∞ 返回 -1。 */
    fun costUb(item: Item): Long {
        val v = value(item)
        if (v.isInfinite()) return -1L
        return (v * VALUE_TO_UB).toLong().coerceAtLeast(0)
    }

    /** 所有算出有限值的物品。 */
    fun allValues(): Map<Item, Double> = nodes.toMap()
}

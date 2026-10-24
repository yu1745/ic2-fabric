package ic2_120.content.upgrade

import ic2_120.compat.time

import ic2_120.content.item.PullingUpgrade
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.item.Item
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import java.util.WeakHashMap

object PullingUpgradeComponent {

    /** 静态全方向列表，避免每次调用重新分配 Direction.values().toList() */
    private val ALL_DIRECTIONS: List<Direction> = Direction.values().toList()

    // ── P5 反馈退避（AE2 式自适应 tick rate，修正版）────────────────────────
    // 干活 → interval -2（加速），空手 → interval +1（减速），clamp [1, MAX_INTERVAL]。
    // 与 AE2 TickManager 不同处（详见 2026-08-26 审查）：
    //  1. 轮转游标用 BE 内自增 fire 计数器，不用 world.time——退避步长与候选数
    //     的 gcd 会把 floorMod(world.time, m) 卡死在固定起点（40 mod 5 = 0）。
    //  2. 配额按实际流逝 tick 数补偿（world.time - lastFired），而非名义 interval，
    //     覆盖 TPS 波动；抽取量仍受输入槽剩余空间自然封顶（稳态吞吐=消耗速率）。
    //  3. MAX_INTERVAL = 20（审查建议从 40 收紧）：共享源容器时降低瞬态物品的
    //     竞争饥饿窗口；玩法代价 = 手动投料最长 1 秒延迟。
    //  4. BE 键弱引用：区块卸载 GC 自动清条目；区块重载/活塞移动 → 新 BE →
    //     interval=1 立即触发，天然自愈邻居通知丢失。
    private const val MIN_INTERVAL = 1
    private const val MAX_INTERVAL = 20

    private class BackoffState {
        var interval: Int = MIN_INTERVAL
        var lastFired: Long = -MAX_INTERVAL.toLong()   // 保证首 tick 必触发
        var fireCount: Long = 0               // BE 内自增游标（轮转公平性）
    }

    private val backoff = WeakHashMap<BlockEntity, BackoffState>()

    /**
     * 扫描升级槽中的所有抽入升级，从相邻容器抽取物品到机器的输入槽。
     * 每个抽入升级使用自己的过滤和方向配置。
     * 轮询语义（对齐 ic2_origin）：每次触发遍历全部 n 个候选方向，每个候选至多抽取
     * elapsed × itemTransferRate(count) 个物品（配额跨输入槽共享，按实际流逝 tick 补偿）；
     * 每次触发起始方向轮转（BE 内计数器游标），避免低吞吐时第一个候选独占。
     * 开启方向过滤时，轮转只在过滤后的方向集内进行。
     * 复用 EjectorUpgradeComponent 的 NBT 读写方法（过滤和方向配置格式一致）。
     */
    fun pullIfUpgraded(
        world: Level,
        pos: BlockPos,
        inventory: Container,
        upgradeSlotIndices: IntArray,
        inputSlotIndices: IntArray
    ) {
        if (inputSlotIndices.isEmpty()) return

        // ── P5 退避门：未到触发时刻直接返回（成本 ≈ 一次 HashMap 查找 + 比较）──
        val machine = inventory as? BlockEntity ?: return pullLegacy(world, pos, inventory, upgradeSlotIndices, inputSlotIndices)
        val st = backoff.getOrPut(machine) { BackoffState() }
        val now = world.time
        val elapsed = now - st.lastFired
        if (elapsed < st.interval) return
        st.lastFired = now
        st.fireCount++

        val rateScale = elapsed.coerceAtLeast(1L)   // 配额按实际流逝补偿（TPS 波动安全）
        val didWork = doPull(world, pos, inventory, machine, upgradeSlotIndices, inputSlotIndices, rateScale, st.fireCount)

        // 反馈调节：干了活加速，空手减速
        st.interval = if (didWork) (st.interval - 2).coerceAtLeast(MIN_INTERVAL)
                      else (st.interval + 1).coerceAtMost(MAX_INTERVAL)
    }

    /** 非 BlockEntity 调用方（不应存在于生产路径，防御保留）：无退避直拉。 */
    private fun pullLegacy(
        world: Level, pos: BlockPos, inventory: Container,
        upgradeSlotIndices: IntArray, inputSlotIndices: IntArray
    ) {
        doPull(world, pos, inventory, null, upgradeSlotIndices, inputSlotIndices, 1L, world.time)
    }

    private fun doPull(
        world: Level,
        pos: BlockPos,
        inventory: Container,
        machine: BlockEntity?,
        upgradeSlotIndices: IntArray,
        inputSlotIndices: IntArray,
        rateScale: Long,
        rotationCursor: Long
    ): Boolean {
        var didWork = false

        // 预解析每个抽入升级的有效方向与速率（多个抽入升级共享同一方向的缓存查找结果）
        val active = mutableListOf<Triple<Int, Item?, List<Direction>>>()
        for (idx in upgradeSlotIndices) {
            val upgradeStack = inventory.getItem(idx)
            if (upgradeStack.isEmpty || upgradeStack.item !is PullingUpgrade) continue
            val rate = EjectorUpgradeComponent.itemTransferRate(upgradeStack.count)
            if (rate <= 0) continue
            val filter = EjectorUpgradeComponent.readFilter(upgradeStack)
            val configuredSides = EjectorUpgradeComponent.readDirections(upgradeStack)
            val dirs = if (configuredSides.isEmpty()) ALL_DIRECTIONS else ALL_DIRECTIONS.filter { it in configuredSides }
            if (dirs.isNotEmpty()) active.add(Triple(rate, filter, dirs))
        }
        if (active.isEmpty()) return false

        // 输入槽全部已满（非空且达到堆叠上限）时，任何物品都无法再抽入，跳过整个方向循环
        if (inputSlotIndices.all { slot ->
                val stack = inventory.getItem(slot)
                !stack.isEmpty && stack.count >= stack.maxStackSize
            }) return false

        // IValidInputItem 快速否定（P1）：配方类机器按 (RecipeType, Item) 布尔缓存
        // 作答，过滤掉必然被 isValid 拒绝的候选，避免 toStack 分配与配方全表扫描。
        val fastReject = machine as? IValidInputItem

        // 邻居源容器引用走 BlockApiCache（BE/方块状态变化时自动失效），避免每 tick 重查 capability
        val caches = if (machine != null) EjectorUpgradeComponent.neighborCaches(world, pos, machine) else null

        for ((rate, filter, dirs) in active) {
            // 先剔除无源容器/不支持抽取的方向，只对有效方向轮转（理由同弹出组件）
            val candidates = ArrayList<Storage<ItemVariant>>(dirs.size)
            for (dir in dirs) {
                val source = if (caches != null) {
                    caches[dir.ordinal]?.find(dir.opposite)
                } else {
                    ItemStorage.SIDED.find(world, pos.relative(dir), dir.opposite)
                } ?: continue
                if (!source.supportsExtraction()) continue
                candidates.add(source)
            }
            if (candidates.isEmpty()) continue

            val m = candidates.size
            // P5 修正：游标用 BE 内自增 fireCount，不用 world.time（gcd 卡死问题）
            val start = Math.floorMod(rotationCursor, m.toLong()).toInt()
            for (i in 0 until m) {
                val source = candidates[(start + i) % m]

                // 该候选本次触发的配额：基础速率 × 流逝补偿，跨所有输入槽共享
                var remainingQuota = rate * rateScale
                // 遍历源容器中的所有物品变体
                for (view in source) {
                    if (remainingQuota <= 0) break
                    if (view.isResourceBlank()) continue
                    val variant = view.resource
                    val item = variant.item

                    // 应用过滤
                    if (filter != null && item != filter) continue
                    // IValidInputItem 快速否定（P1）：零分配，走布尔缓存
                    if (fastReject != null && !fastReject.isValidInputItem(item)) continue

                    // 找到可以接受此物品的输入槽
                    for (slotIndex in inputSlotIndices) {
                        if (remainingQuota <= 0) break
                        val current = inventory.getItem(slotIndex)
                        if (!current.isEmpty) {
                            if (current.item != item) continue
                            if (current.count >= current.maxStackSize) continue
                        }

                        // 检查机器是否接受该物品（完整语义裁决，仅对放行候选执行一次）
                        if (!inventory.canPlaceItem(slotIndex, variant.toStack(1))) continue

                        val space = if (current.isEmpty) current.maxStackSize else current.maxStackSize - current.count
                        if (space <= 0) continue

                        // 受每候选配额限制，从源容器中抽取物品
                        val amount = minOf(remainingQuota, space.toLong(), view.amount).toInt()
                        if (amount <= 0) continue

                        Transaction.openOuter().use { tx ->
                            val extracted = source.extract(variant, amount.toLong(), tx)
                            if (extracted > 0) {
                                tx.commit()
                                remainingQuota -= extracted
                                didWork = true
                                // 将物品放入输入槽
                                if (current.isEmpty) {
                                    inventory.setItem(slotIndex, variant.toStack(extracted.toInt()))
                                } else {
                                    val newStack = current.copy()
                                    newStack.count += extracted.toInt()
                                    inventory.setItem(slotIndex, newStack)
                                }
                            }
                        }
                    }
                }
            }
        }
        return didWork
    }
}

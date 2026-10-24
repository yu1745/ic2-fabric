package ic2_120.content.upgrade

import net.minecraft.core.component.DataComponents
import ic2_120.compat.orCreateNbt

import ic2_120.compat.time

import ic2_120.content.item.EjectorUpgrade
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import java.util.WeakHashMap
import kotlin.math.pow

object EjectorUpgradeComponent {
    private const val NBT_ITEM_FILTER = "PipeItemFilter"
    private const val NBT_DIRECTION = "PipeItemDirection"
    private const val NBT_DIRECTIONS = "PipeItemDirections"

    private data class EjectorConfig(val filter: Item?, val sides: Set<Direction>, val count: Int)

    // ── P5 反馈退避（与 PullingUpgradeComponent 同款，独立分表不共用）──
    private const val MIN_INTERVAL = 1
    private const val MAX_INTERVAL = 20

    private class BackoffState {
        var interval: Int = MIN_INTERVAL
        var lastFired: Long = -MAX_INTERVAL.toLong()
        var fireCount: Long = 0
    }

    private val backoff = WeakHashMap<BlockEntity, BackoffState>()

    /**
     * 对齐 ic2_origin：物品传输速率 = 4^(min(count, 4) - 1) 个/tick/候选，count=0 时返回 0。
     */
    fun itemTransferRate(upgradeCount: Int): Int {
        if (upgradeCount <= 0) return 0
        val capped = minOf(upgradeCount, 4)
        return 4.0.pow(capped - 1).toInt()
    }

    /** 静态全方向列表，避免每次调用重新分配 Direction.values().toList() */
    private val ALL_DIRECTIONS: List<Direction> = Direction.values().toList()

    /**
     * 每台机器的邻居 itemStorage 缓存（按 Direction.ordinal 索引，懒创建），抽入/弹出共用。
     *
     * 以机器 BlockEntity 为键（identity）：BE 卸载被 GC 后条目自动消失，无需显式监听卸载。
     * BlockApiCache 内部通过 ServerBlockEntityEvents（邻居 BE 加载/卸载）与方块状态变化
     * 自动失效——邻居被替换/移除/重载后下次 find 自动重新查找，无需每 tick 重查 capability。
     * 机器自身被活塞移动或区块重载时 BE 实例会重建，自动得到新的缓存条目。
     *
     * 注意：ItemStorage.SIDED 的 context 类型为 @Nullable Direction，因此缓存泛型参数用 Direction?。
     */
    private val neighborCache = WeakHashMap<BlockEntity, Array<BlockApiCache<Storage<ItemVariant>, Direction?>?>>()

    /**
     * 获取（必要时创建）机器 6 个方向的 BlockApiCache。
     * world 非服务端或 inventory 非 BlockEntity 时返回 null，调用方回退到直接 find（等价于旧行为）。
     */
    fun neighborCaches(
        world: Level,
        pos: BlockPos,
        machine: BlockEntity
    ): Array<BlockApiCache<Storage<ItemVariant>, Direction?>?>? {
        val serverWorld = world as? ServerLevel ?: return null
        val caches = neighborCache.getOrPut(machine) {
            arrayOfNulls<BlockApiCache<Storage<ItemVariant>, Direction?>>(Direction.values().size)
        }
        for (dir in Direction.values()) {
            if (caches[dir.ordinal] == null) {
                caches[dir.ordinal] = BlockApiCache.create(ItemStorage.SIDED, serverWorld, pos.relative(dir))
            }
        }
        return caches
    }

    /**
     * 统一入口：扫描升级槽中的所有弹出升级，逐个独立弹出 outputSlotIndices 中的物品。
     * 每个弹出升级使用自己的过滤和方向配置。
     * 轮询语义（对齐 ic2_origin，2026-08-26 P4/P5 修订）：每次触发遍历全部 n 个候选方向，
     * 每个候选至多传输 elapsed × itemTransferRate(count) 个物品（配额跨输出槽共享，按实际
     * 流逝 tick 补偿）；每次触发起始方向轮转（BE 内计数器游标），避免低吞吐时第一个候选独占。
     * 开启方向过滤时，轮转只在过滤后的方向集内进行。整个触发共用一个事务（P4）。
     * 使用 Fabric Transfer API 查找目标容器，兼容 vanilla Container 和 modded Storage。
     * 空闲退避（P5）：空手 1t/tick 递增至最长 20t 一跳，干活即逐步回到每 tick。
     */
    fun ejectIfUpgraded(
        world: Level,
        pos: BlockPos,
        inventory: Container,
        upgradeSlotIndices: IntArray,
        outputSlotIndices: IntArray
    ) {
        if (outputSlotIndices.isEmpty()) return

        // ── P5 退避门：未到触发时刻直接返回 ──
        val machine = inventory as? BlockEntity
        var st: BackoffState? = null
        var elapsed = 1L
        if (machine != null) {
            st = backoff.getOrPut(machine) { BackoffState() }
            val now = world.time
            elapsed = now - st.lastFired
            if (elapsed < st.interval) return
            st.lastFired = now
            st.fireCount++
        }

        val didWork = doEject(world, pos, inventory, machine, upgradeSlotIndices, outputSlotIndices,
            elapsed.coerceAtLeast(1L), machine?.let { backoff[it]!!.fireCount } ?: world.time)

        // 反馈调节
        if (st != null) {
            st.interval = if (didWork) (st.interval - 2).coerceAtLeast(MIN_INTERVAL)
                          else (st.interval + 1).coerceAtMost(MAX_INTERVAL)
        }
    }

    private fun doEject(
        world: Level,
        pos: BlockPos,
        inventory: Container,
        machine: BlockEntity?,
        upgradeSlotIndices: IntArray,
        outputSlotIndices: IntArray,
        rateScale: Long,
        rotationCursor: Long
    ): Boolean {
        var didWork = false

        val configs = mutableListOf<EjectorConfig>()
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getItem(idx)
            if (!stack.isEmpty && stack.item is EjectorUpgrade) {
                configs.add(EjectorConfig(readFilter(stack), readDirections(stack), stack.count))
            }
        }
        if (configs.isEmpty()) return false

        // 预解析每个升级配置的有效方向与速率（多个弹出升级共享同一方向的缓存查找结果）
        val active = mutableListOf<Triple<EjectorConfig, Int, List<Direction>>>()
        for (config in configs) {
            val rate = itemTransferRate(config.count)
            if (rate <= 0) continue
            val dirs = if (config.sides.isEmpty()) ALL_DIRECTIONS else ALL_DIRECTIONS.filter { it in config.sides }
            if (dirs.isEmpty()) continue
            active.add(Triple(config, rate, dirs))
        }
        if (active.isEmpty()) return false

        // 输出槽全空时无需弹出，跳过整个方向循环
        if (outputSlotIndices.all { inventory.getItem(it).isEmpty }) return false

        val caches = if (machine != null) neighborCaches(world, pos, machine) else null

        // P4 事务合并：整个 doEject 一个 outer transaction，全部槽位先模拟 insert、
        // 统一 commit；任一 insert 失败不影响其他槽位（moved=0 只是不累加）。
        // 顺带消除旧实现「insert 返回 0 也 commit 空事务」的浪费。
        val tx = Transaction.openOuter()
        try {
            for ((config, rate, dirs) in active) {
                val candidates = ArrayList<Storage<ItemVariant>>(dirs.size)
                for (dir in dirs) {
                    val target = if (caches != null) {
                        caches[dir.ordinal]?.find(dir.opposite)
                    } else {
                        ItemStorage.SIDED.find(world, pos.relative(dir), dir.opposite)
                    } ?: continue
                    candidates.add(target)
                }
                if (candidates.isEmpty()) continue

                val m = candidates.size
                // P5 修正：BE 内计数器游标，不用 world.time（gcd 卡死）
                val start = Math.floorMod(rotationCursor, m.toLong()).toInt()
                for (i in 0 until m) {
                    val target = candidates[(start + i) % m]

                    // 配额：基础速率 × 流逝补偿，跨所有输出槽共享
                    var remainingQuota = rate * rateScale
                    for (slotIndex in outputSlotIndices) {
                        if (remainingQuota <= 0) break
                        val stack = inventory.getItem(slotIndex)
                        if (stack.isEmpty) continue
                        if (config.filter != null && stack.item != config.filter) continue

                        val variant = ItemVariant.of(stack)
                        val move = minOf(remainingQuota, stack.count.toLong())
                        val moved = target.insert(variant, move, tx)
                        if (moved <= 0) continue

                        remainingQuota -= moved
                        didWork = true
                        val left = stack.count - moved.toInt()
                        if (left <= 0) {
                            inventory.setItem(slotIndex, ItemStack.EMPTY)
                        } else {
                            val newStack = stack.copy()
                            newStack.count = left
                            inventory.setItem(slotIndex, newStack)
                        }
                    }
                }
            }
            if (didWork) tx.commit()
        } finally {
            tx.close()
        }
        return didWork
    }

    fun readFilter(stack: ItemStack): Item? {
        val nbt = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return null
        val raw = nbt.getString(NBT_ITEM_FILTER)
        if (raw.isNullOrBlank()) return null
        val id = ResourceLocation.tryParse(raw) ?: return null
        return if (BuiltInRegistries.ITEM.containsKey(id)) BuiltInRegistries.ITEM.get(id) else null
    }

    fun writeFilter(stack: ItemStack, item: Item?) {
        val nbt = stack.orCreateNbt
        if (item == null) {
            nbt.remove(NBT_ITEM_FILTER)
            return
        }
        val id = BuiltInRegistries.ITEM.getKey(item)
        if (id.path != "air") nbt.putString(NBT_ITEM_FILTER, id.toString())
        else nbt.remove(NBT_ITEM_FILTER)
    }

    fun readDirection(stack: ItemStack): Direction? {
        return readDirections(stack).singleOrNull()
    }

    fun writeDirection(stack: ItemStack, side: Direction?) {
        writeDirections(stack, if (side == null) emptySet() else setOf(side))
    }

    /** 空集合表示任意方向；同时兼容旧版本的单方向 NBT。 */
    fun readDirections(stack: ItemStack): Set<Direction> {
        val nbt = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return emptySet()
        val list = nbt.getList(NBT_DIRECTIONS, net.minecraft.nbt.Tag.TAG_STRING.toInt())
        if (!list.isEmpty()) {
            return list.mapNotNull { Direction.byName(it.asString()) }.toSet()
        }

        val raw = nbt.getString(NBT_DIRECTION)
        if (raw.isNullOrBlank()) return emptySet()
        return Direction.byName(raw.lowercase())?.let { setOf(it) } ?: emptySet()
    }

    fun writeDirections(stack: ItemStack, sides: Set<Direction>) {
        val nbt = stack.orCreateNbt
        nbt.remove(NBT_DIRECTION)
        if (sides.isEmpty()) {
            nbt.remove(NBT_DIRECTIONS)
            nbt.remove(NBT_DIRECTION)
            return
        }
        val list = net.minecraft.nbt.ListTag()
        for (side in sides) {
            list.add(net.minecraft.nbt.StringTag.of(side.name.lowercase()))
        }
        nbt.put(NBT_DIRECTIONS, list)
    }

    fun nextDirection(current: Direction?): Direction? {
        return when (current) {
            null -> Direction.DOWN
            Direction.DOWN -> Direction.UP
            Direction.UP -> Direction.NORTH
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.WEST
            Direction.WEST -> Direction.EAST
            Direction.EAST -> null
        }
    }
}

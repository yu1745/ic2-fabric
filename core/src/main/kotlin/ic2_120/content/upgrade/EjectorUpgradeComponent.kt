package ic2_120.content.upgrade

import ic2_120.content.item.EjectorUpgrade
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.entity.BlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.WeakHashMap
import kotlin.math.pow

object EjectorUpgradeComponent {
    private const val NBT_ITEM_FILTER = "PipeItemFilter"
    private const val NBT_DIRECTION = "PipeItemDirection"
    private const val NBT_DIRECTIONS = "PipeItemDirections"

    private data class EjectorConfig(val filter: Item?, val sides: Set<Direction>, val count: Int)

    /** 升级配置解析缓存：key = 升级槽里的 ItemStack（item+NBT 不变即命中，count 变化只 miss 一次）。
     *  每 tick 热路径（eject/pull）不再重复解析 NBT + Registry 查询；LRU 上限 256 防泄漏。 */
    private data class ParsedUpgradeConfig(val filter: Item?, val sides: Set<Direction>)

    private val configCache = object : LinkedHashMap<ItemStack, ParsedUpgradeConfig>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ItemStack, ParsedUpgradeConfig>): Boolean = size > 256
    }

    private fun parsedConfig(stack: ItemStack): ParsedUpgradeConfig {
        if (stack.isEmpty) return ParsedUpgradeConfig(null, emptySet())
        return configCache.getOrPut(stack) { ParsedUpgradeConfig(parseFilter(stack), parseDirections(stack)) }
    }

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
        world: World,
        pos: BlockPos,
        machine: BlockEntity
    ): Array<BlockApiCache<Storage<ItemVariant>, Direction?>?>? {
        val serverWorld = world as? ServerWorld ?: return null
        val caches = neighborCache.getOrPut(machine) {
            arrayOfNulls<BlockApiCache<Storage<ItemVariant>, Direction?>>(Direction.values().size)
        }
        for (dir in Direction.values()) {
            if (caches[dir.ordinal] == null) {
                caches[dir.ordinal] = BlockApiCache.create(ItemStorage.SIDED, serverWorld, pos.offset(dir))
            }
        }
        return caches
    }

    /**
     * 统一入口：扫描升级槽中的所有弹出升级，逐个独立弹出 outputSlotIndices 中的物品。
     * 每个弹出升级使用自己的过滤和方向配置。
     * 轮询语义（对齐 ic2_origin）：每 tick 遍历全部 n 个候选方向，每个候选至多传输
     * itemTransferRate(count) 个物品（配额跨输出槽共享）；每 tick 起始方向轮转一次，
     * 避免低吞吐时第一个候选独占。开启方向过滤时，轮转只在过滤后的方向集内进行。
     * 使用 Fabric Transfer API 查找目标容器，兼容 vanilla Inventory 和 modded Storage。
     */
    fun ejectIfUpgraded(
        world: World,
        pos: BlockPos,
        inventory: Inventory,
        upgradeSlotIndices: IntArray,
        outputSlotIndices: IntArray
    ) {
        if (outputSlotIndices.isEmpty()) return

        val configs = mutableListOf<EjectorConfig>()
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (stack.isEmpty) continue
            if (stack.item is EjectorUpgrade) {
                val parsed = parsedConfig(stack)
                configs.add(EjectorConfig(parsed.filter, parsed.sides, stack.count))
            }
        }
        if (configs.isEmpty()) return

        // 预解析每个升级配置的有效方向与速率（多个弹出升级共享同一方向的缓存查找结果）
        val active = mutableListOf<Triple<EjectorConfig, Int, List<Direction>>>()
        for (config in configs) {
            val rate = itemTransferRate(config.count)
            if (rate <= 0) continue
            val dirs = if (config.sides.isEmpty()) ALL_DIRECTIONS else ALL_DIRECTIONS.filter { it in config.sides }
            if (dirs.isEmpty()) continue
            active.add(Triple(config, rate, dirs))
        }
        if (active.isEmpty()) return

        // 输出槽全空时无需弹出，跳过整个方向循环
        if (outputSlotIndices.all { inventory.getStack(it).isEmpty }) return

        // 邻居容器引用走 BlockApiCache（BE/方块状态变化时自动失效），避免每 tick 重查 capability；
        // 无法缓存（非服务端/非 BE 调用）时回退到直接 find。
        val machine = inventory as? BlockEntity
        val caches = if (machine != null) neighborCaches(world, pos, machine) else null

        for ((config, rate, dirs) in active) {
            // 先剔除无目标容器的空方向，只对有效方向轮转：空方向穿插在列表里会破坏轮转
            // 对称性（第一个有效方向由 (index−start) mod n 决定，靠前方向系统性占优），
            // 有效列表长度 m 的起点轮转（world.time 即游标）让每个方向在每个处理位置
            // 各出现 1/m 次，任意 m、任意物品量下竞争分配完全均等。
            val candidates = ArrayList<Storage<ItemVariant>>(dirs.size)
            for (dir in dirs) {
                val target = if (caches != null) {
                    caches[dir.ordinal]?.find(dir.opposite)
                } else {
                    ItemStorage.SIDED.find(world, pos.offset(dir), dir.opposite)
                } ?: continue
                candidates.add(target)
            }
            if (candidates.isEmpty()) continue

            val m = candidates.size
            val start = Math.floorMod(world.time, m.toLong()).toInt()
            for (i in 0 until m) {
                val target = candidates[(start + i) % m]

                // 该候选本次 tick 的配额，跨所有输出槽共享（对齐原版 transfer(amount) 语义）
                var remainingQuota = rate
                for (slotIndex in outputSlotIndices) {
                    if (remainingQuota <= 0) break
                    val stack = inventory.getStack(slotIndex)
                    if (stack.isEmpty) continue
                    if (config.filter != null && stack.item != config.filter) continue

                    val variant = ItemVariant.of(stack)
                    val move = minOf(remainingQuota.toLong(), stack.count.toLong())
                    val tx = Transaction.openOuter()
                    val moved = target.insert(variant, move, tx)
                    tx.commit()
                    if (moved <= 0) continue

                    remainingQuota -= moved.toInt()
                    val left = stack.count - moved.toInt()
                    if (left <= 0) {
                        inventory.setStack(slotIndex, ItemStack.EMPTY)
                    } else {
                        val newStack = stack.copy()
                        newStack.count = left
                        inventory.setStack(slotIndex, newStack)
                    }
                }
            }
        }
    }

    fun readFilter(stack: ItemStack): Item? = parsedConfig(stack).filter

    private fun parseFilter(stack: ItemStack): Item? {
        val nbt = stack.nbt ?: return null
        val raw = nbt.getString(NBT_ITEM_FILTER)
        if (raw.isNullOrBlank()) return null
        val id = Identifier.tryParse(raw) ?: return null
        return if (Registries.ITEM.containsId(id)) Registries.ITEM.get(id) else null
    }

    fun writeFilter(stack: ItemStack, item: Item?) {
        val nbt = stack.orCreateNbt
        if (item == null) {
            nbt.remove(NBT_ITEM_FILTER)
            return
        }
        val id = Registries.ITEM.getId(item)
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
    fun readDirections(stack: ItemStack): Set<Direction> = parsedConfig(stack).sides

    private fun parseDirections(stack: ItemStack): Set<Direction> {
        val nbt = stack.nbt ?: return emptySet()
        val list = nbt.getList(NBT_DIRECTIONS, net.minecraft.nbt.NbtElement.STRING_TYPE.toInt())
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
        val list = net.minecraft.nbt.NbtList()
        for (side in sides) {
            list.add(net.minecraft.nbt.NbtString.of(side.name.lowercase()))
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

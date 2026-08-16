package ic2_120.content.upgrade

import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidEjectorUpgrade
import ic2_120.content.item.FluidPullingUpgrade
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventory
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.block.entity.BlockEntity
import java.util.WeakHashMap
import kotlin.math.min
import kotlin.math.pow

private val DIRECTION_ORDER = listOf(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

object FluidPipeUpgradeComponent {
    private const val NBT_FILTER = "PipeFluidFilter"
    private const val NBT_DIRECTIONS = "PipeFluidDirections"

    /** 流体升级配置解析缓存（同 EjectorUpgradeComponent 的缓存策略） */
    private data class ParsedFluidConfig(val filter: Fluid?, val sides: Set<Direction>)

    private val configCache = object : LinkedHashMap<ItemStack, ParsedFluidConfig>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ItemStack, ParsedFluidConfig>): Boolean = size > 256
    }

    /**
     * 已应用配置快照（按机器 BE 缓存）：升级槽引用+数量未变 → 配置必然未变，
     * 直接跳过整个 apply（稳态零扫描、零比较、零分配）。
     * 槽被 setStack 替换（引用变）或 count 变化时自动失效重算。
     */
    private class AppliedFluidConfig(
        val stacks: Array<ItemStack?>,
        val counts: IntArray,
        val provider: Boolean,
        val receiver: Boolean,
        val providerFilter: Fluid?,
        val receiverFilter: Fluid?,
        val providerSides: Set<Direction>,
        val receiverSides: Set<Direction>,
        val ejectorCount: Int,
        val pullingCount: Int
    )

    private val appliedCache = WeakHashMap<BlockEntity, AppliedFluidConfig>()

    private fun parsedConfig(stack: ItemStack): ParsedFluidConfig {
        if (stack.isEmpty) return ParsedFluidConfig(null, emptySet())
        return configCache.getOrPut(stack) { ParsedFluidConfig(parseFilter(stack), parseDirections(stack)) }
    }

    /**
     * 统一入口：机器同时具备 Inventory 与 IFluidPipeUpgradeSupport 时，按升级槽应用流体管道升级。
     */
    fun <T> apply(machine: T, upgradeSlotIndices: IntArray) where T : Inventory, T : IFluidPipeUpgradeSupport {
        apply(machine as Inventory, upgradeSlotIndices, machine as Any)
    }

    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is IFluidPipeUpgradeSupport) return

        // 快速路径：升级槽引用+数量未变 → 上次结果仍然有效，跳过全部重算
        val be = inventory as? BlockEntity
        val cached = be?.let { appliedCache[it] }
        if (cached != null && cached.stacks.size == upgradeSlotIndices.size) {
            var same = true
            for (i in upgradeSlotIndices.indices) {
                val s = inventory.getStack(upgradeSlotIndices[i])
                if (cached.stacks[i] !== s || cached.counts[i] != s.count) {
                    same = false
                    break
                }
            }
            if (same) return
        }

        var provider = false
        var receiver = false
        var providerFilter: Fluid? = null
        var receiverFilter: Fluid? = null
        var providerSides = emptySet<Direction>()
        var receiverSides = emptySet<Direction>()
        var ejectorCount = 0
        var pullingCount = 0

        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (stack.isEmpty) continue
            when (stack.item) {
                is FluidEjectorUpgrade -> {
                    provider = true
                    ejectorCount++
                    if (providerFilter == null) providerFilter = readFilter(stack)
                    if (providerSides.isEmpty()) providerSides = readDirections(stack)
                }
                is FluidPullingUpgrade -> {
                    receiver = true
                    pullingCount++
                    if (receiverFilter == null) receiverFilter = readFilter(stack)
                    if (receiverSides.isEmpty()) receiverSides = readDirections(stack)
                }
            }
        }

        machine.fluidPipeProviderEnabled = provider
        machine.fluidPipeReceiverEnabled = receiver
        machine.fluidPipeProviderFilter = providerFilter
        machine.fluidPipeReceiverFilter = receiverFilter
        machine.fluidPipeProviderSides = providerSides.toMutableSet()
        machine.fluidPipeReceiverSides = receiverSides.toMutableSet()
        machine.fluidPipeEjectorCount = ejectorCount
        machine.fluidPipePullingCount = pullingCount

        if (be != null) {
            val stacks = arrayOfNulls<ItemStack>(upgradeSlotIndices.size)
            val counts = IntArray(upgradeSlotIndices.size)
            for (i in upgradeSlotIndices.indices) {
                stacks[i] = inventory.getStack(upgradeSlotIndices[i])
                counts[i] = stacks[i]?.count ?: 0
            }
            appliedCache[be] = AppliedFluidConfig(
                stacks, counts, provider, receiver, providerFilter, receiverFilter,
                providerSides, receiverSides, ejectorCount, pullingCount
            )
        }
    }

    fun readFilter(stack: ItemStack): Fluid? = parsedConfig(stack).filter

    private fun parseFilter(stack: ItemStack): Fluid? {
        val nbt = stack.nbt ?: return null
        val raw = nbt.getString(NBT_FILTER)
        if (raw.isNullOrBlank()) return null
        val id = Identifier.tryParse(raw) ?: return null
        return if (Registries.FLUID.containsId(id)) Registries.FLUID.get(id) else null
    }

    fun writeFilter(stack: ItemStack, fluid: Fluid?) {
        val nbt = stack.orCreateNbt
        if (fluid == null) {
            nbt.remove(NBT_FILTER)
            return
        }
        val id = Registries.FLUID.getId(fluid)
        if (id.path != "empty") {
            nbt.putString(NBT_FILTER, id.toString())
        } else {
            nbt.remove(NBT_FILTER)
        }
    }

    fun readDirections(stack: ItemStack): Set<Direction> = parsedConfig(stack).sides

    private fun parseDirections(stack: ItemStack): Set<Direction> {
        val nbt = stack.nbt ?: return emptySet()
        val list = nbt.getList(NBT_DIRECTIONS, net.minecraft.nbt.NbtElement.STRING_TYPE.toInt())
        if (list.isEmpty()) return emptySet()
        return list.mapNotNull { Direction.byName(it.asString()) }.toSet()
    }

    fun writeDirections(stack: ItemStack, sides: Set<Direction>) {
        val nbt = stack.orCreateNbt
        if (sides.isEmpty()) {
            nbt.remove(NBT_DIRECTIONS)
            return
        }
        val list = net.minecraft.nbt.NbtList()
        for (side in sides) {
            list.add(net.minecraft.nbt.NbtString.of(side.name.lowercase()))
        }
        nbt.put(NBT_DIRECTIONS, list)
    }

    /**
     * 将储罐中的流体弹出到相邻方块。
     * 轮询语义：每 tick 遍历全部候选方向，每个候选至多传输 ratePerTick；每 tick 起始方向
     * 轮转一次，避免低吞吐时第一个候选独占；开启方向过滤时，轮转只在过滤后的方向集内进行。
     * @param upgradeCount 该机器上 fluid_ejector_upgrade 的数量（决定速率）
     */
    fun ejectFluidToNeighbors(
        world: World,
        pos: BlockPos,
        tank: SingleVariantStorage<FluidVariant>,
        filter: Fluid?,
        configuredSides: Set<Direction> = emptySet(),
        blockedFace: Direction? = null,
        upgradeCount: Int = 0
    ) {
        if (tank.amount <= 0L || tank.variant.isBlank) return
        val ratePerTick = fluidTransferRate(upgradeCount)
        if (ratePerTick <= 0L) return
        val dirs = DIRECTION_ORDER.filter {
            (blockedFace == null || it != blockedFace) && (configuredSides.isEmpty() || it in configuredSides)
        }
        if (dirs.isEmpty()) return

        // 每 tick 轮转起始方向，使 n 个候选轮流获得优先服务
        val start = Math.floorMod(world.time, dirs.size.toLong()).toInt()
        for (i in 0 until dirs.size) {
            val dir = dirs[(start + i) % dirs.size]
            val neighbor = FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue
            val resource = tank.variant
            if (filter != null && resource.fluid != filter) continue
            if (tank.amount <= 0L) break
            val maxPerTick = minOf(ratePerTick, tank.amount)

            // 先 dry-run 探测邻居本次能接收的量，避免多抽后无法回退造成凭空消失
            val neighborSpace = Transaction.openOuter().use { tx -> neighbor.insert(resource, maxPerTick, tx) }
            if (neighborSpace <= 0L) continue

            Transaction.openOuter().use { tx ->
                val extracted = tank.extract(resource, neighborSpace, tx)
                if (extracted <= 0L) return@use
                val accepted = neighbor.insert(resource, extracted, tx)
                if (accepted <= 0L) return@use
                if (accepted < extracted) {
                    // 回退失败（如输出罐 canInsert=false）时放弃本次传输：不提交任何部分结果，绝不凭空销毁流体
                    val refunded = tank.insert(resource, extracted - accepted, tx)
                    if (refunded < extracted - accepted) return@use
                }
                tx.commit()
            }
            if (tank.amount <= 0L) break
        }
    }

    /**
     * 从相邻方块的 FluidStorage 主动抽取流体到指定的储罐。
     * 由安装了 fluid_pulling_upgrade 的机器在 tick 中调用。
     *
     * [tank] 使用通用 [Storage] 接口，支持 SingleVariantStorage 和多流体储罐。
     * 通过 dry-run transaction 确定剩余容量，不依赖 capacity 字段。
     * 轮询语义：每 tick 遍历全部候选方向，每个候选至多抽取 ratePerTick；每 tick 起始方向
     * 轮转一次，避免低吞吐时第一个候选独占；开启方向过滤时，轮转只在过滤后的方向集内进行。
     * @param upgradeCount 该机器上 fluid_pulling_upgrade 的数量（决定速率）
     */
    fun pullFluidFromNeighbors(
        world: World,
        pos: BlockPos,
        tank: Storage<FluidVariant>,
        filter: Fluid?,
        configuredSides: Set<Direction> = emptySet(),
        blockedFace: Direction? = null,
        upgradeCount: Int = 0
    ) {
        val ratePerTick = fluidTransferRate(upgradeCount)
        if (ratePerTick <= 0L) return
        val dirs = DIRECTION_ORDER.filter {
            (blockedFace == null || it != blockedFace) && (configuredSides.isEmpty() || it in configuredSides)
        }
        if (dirs.isEmpty()) return

        // 每 tick 轮转起始方向，使 n 个候选轮流获得优先服务
        val start = Math.floorMod(world.time, dirs.size.toLong()).toInt()
        for (i in 0 until dirs.size) {
            val dir = dirs[(start + i) % dirs.size]
            val neighborPos = pos.offset(dir)
            val neighbor = FluidStorage.SIDED.find(world, neighborPos, dir.opposite) ?: continue
            if (!neighbor.supportsExtraction()) continue

            val resourceToPull = resolveResourceToPull(tank, neighbor, filter) ?: continue

            val maxPerTick = ratePerTick

            // dry-run extract
            val extractable = Transaction.openOuter().use { tx ->
                neighbor.extract(resourceToPull, maxPerTick, tx)
            }
            if (extractable <= 0) continue

            // dry-run insert into our tank
            val insertable = Transaction.openOuter().use { tx ->
                tank.insert(resourceToPull, extractable, tx)
            }
            if (insertable <= 0) continue

            // actual transfer
            Transaction.openOuter().use { tx ->
                val extracted = neighbor.extract(resourceToPull, insertable, tx)
                if (extracted <= 0) return@use
                val inserted = tank.insert(resourceToPull, extracted, tx)
                if (inserted <= 0) return@use
                if (inserted < extracted) return@use // 未能全部放入则放弃，避免竞态下凭空消失
                tx.commit()
            }
        }
    }

    /**
     * 决定本次要抽取的流体种类：
     * 1. filter 优先（升级配置的过滤）
     * 2. 储罐已有的流体（如果储罐只接受一种）
     * 3. 从邻居扫描第一个可提取的流体
     */
    private fun resolveResourceToPull(
        tank: Storage<FluidVariant>,
        neighbor: Storage<FluidVariant>,
        filter: Fluid?
    ): FluidVariant? {
        if (filter != null) return FluidVariant.of(filter)

        // 检查 tank 是否已有特定流体（通过 dry-run 确定能插入什么来反推偏好）
        // 对于 SingleVariantStorage，我们看它当前存的流体
        if (tank is SingleVariantStorage && !tank.variant.isBlank) {
            return tank.variant
        }

        // 扫描邻居的第一个可用流体
        for (view in neighbor) {
            if (!view.isResourceBlank && view.amount > 0) {
                val resource = view.resource
                // 确认 tank 能接受
                val accepted = Transaction.openOuter().use { tx ->
                    tank.insert(resource, 1L, tx)
                }
                if (accepted > 0) return resource
            }
        }
        return null
    }

    /**
     * 根据流体升级数量计算每 tick 每方向的传输速率。
     * 对齐 ic2_origin：50 mB/t × 4^(min(count, 4) - 1)，count=0 时返回 0。
     */
    private fun fluidTransferRate(upgradeCount: Int): Long {
        if (upgradeCount <= 0) return 0L
        val capped = min(upgradeCount, 4)
        val mbPerTick = (50.0 * 4.0.pow(capped - 1)).toLong()
        return mbPerTick * FluidConstants.BUCKET / 1000
    }

    /** 逐次添加方向，满 6 个后清空（任意）。空 Set = 所有方向。 */
    fun nextDirections(current: Set<Direction>): Set<Direction> {
        if (current.size >= DIRECTION_ORDER.size) return emptySet()
        val nextDir = DIRECTION_ORDER.firstOrNull { it !in current } ?: return emptySet()
        return current + nextDir
    }

    /**
     * 从物品堆中读取其所含的流体类型。
     * 用于升级 GUI 中检测容器内的流体。
     *
     * 检测优先级：
     * 1. Fabric Transfer API（流体单元、模组容器等）
     * 2. 原版桶硬编码映射（水桶、熔岩桶）
     * 3. 本模组桶硬编码映射（[ModFluids.Ic2BucketItem]）
     */
    fun readFluidFromItemStack(stack: ItemStack): Fluid? {
        if (stack.isEmpty) return null

        // 1. Fabric Transfer API
        val storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack))
        val view = storage?.iterator()?.asSequence()?.firstOrNull { !it.resource.isBlank && it.amount > 0L }
        val fabricFluid = view?.resource?.fluid
        if (fabricFluid != null && fabricFluid != Fluids.EMPTY) return fabricFluid

        // 2. 原版桶 → 3. 本模组桶
        return when (val item = stack.item) {
            Items.WATER_BUCKET -> Fluids.WATER
            Items.LAVA_BUCKET -> Fluids.LAVA
            else -> ModFluids.getFluidFromModBucket(item)
        }
    }
}

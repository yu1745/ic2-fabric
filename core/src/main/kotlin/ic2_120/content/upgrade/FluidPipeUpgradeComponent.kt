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
import ic2_120.editCustomData
import ic2_120.getCustomData
import kotlin.math.min
import kotlin.math.pow

private val DIRECTION_ORDER = listOf(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

object FluidPipeUpgradeComponent {
    private const val NBT_FILTER = "PipeFluidFilter"
    private const val NBT_DIRECTIONS = "PipeFluidDirections"

    /**
     * 统一入口：机器同时具备 Inventory 与 IFluidPipeUpgradeSupport 时，按升级槽应用流体管道升级。
     */
    fun <T> apply(machine: T, upgradeSlotIndices: IntArray) where T : Inventory, T : IFluidPipeUpgradeSupport {
        apply(machine as Inventory, upgradeSlotIndices, machine as Any)
    }

    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is IFluidPipeUpgradeSupport) return

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
    }

    fun readFilter(stack: ItemStack): Fluid? {
        val nbt = stack.getCustomData() ?: return null
        val raw = nbt.getString(NBT_FILTER)
        if (raw.isNullOrBlank()) return null
        val id = Identifier.tryParse(raw) ?: return null
        return if (Registries.FLUID.containsId(id)) Registries.FLUID.get(id) else null
    }

    fun writeFilter(stack: ItemStack, fluid: Fluid?) {
        stack.editCustomData { nbt ->
            if (fluid == null) {
                nbt.remove(NBT_FILTER)
                return@editCustomData
            }
            val id = Registries.FLUID.getId(fluid)
            if (id.path != "empty") {
                nbt.putString(NBT_FILTER, id.toString())
            } else {
                nbt.remove(NBT_FILTER)
            }
        }
    }

    fun readDirections(stack: ItemStack): Set<Direction> {
        val nbt = stack.getCustomData() ?: return emptySet()
        val list = nbt.getList(NBT_DIRECTIONS, net.minecraft.nbt.NbtElement.STRING_TYPE.toInt())
        if (list.isEmpty()) return emptySet()
        return list.mapNotNull { Direction.byName(it.asString()) }.toSet()
    }

    fun writeDirections(stack: ItemStack, sides: Set<Direction>) {
        stack.editCustomData { nbt ->
            if (sides.isEmpty()) {
                nbt.remove(NBT_DIRECTIONS)
                return@editCustomData
            }
            val list = net.minecraft.nbt.NbtList()
            for (side in sides) {
                list.add(net.minecraft.nbt.NbtString.of(side.name.lowercase()))
            }
            nbt.put(NBT_DIRECTIONS, list)
        }
    }

    /**
     * 将储罐中的流体弹出到相邻方块。
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
        for (dir in Direction.values()) {
            if (blockedFace != null && dir == blockedFace) continue
            if (configuredSides.isNotEmpty() && dir !in configuredSides) continue
            val neighbor = FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue
            val resource = tank.variant
            if (filter != null && resource.fluid != filter) continue
            val maxPerTick = minOf(ratePerTick, tank.amount)
            Transaction.openOuter().use { tx ->
                val extracted = tank.extract(resource, maxPerTick, tx)
                if (extracted <= 0L) return@use
                val accepted = neighbor.insert(resource, extracted, tx)
                if (accepted <= 0L) return@use
                if (accepted < extracted) {
                    tank.insert(resource, extracted - accepted, tx)
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
        for (dir in Direction.values()) {
            if (blockedFace != null && dir == blockedFace) continue
            if (configuredSides.isNotEmpty() && dir !in configuredSides) continue
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

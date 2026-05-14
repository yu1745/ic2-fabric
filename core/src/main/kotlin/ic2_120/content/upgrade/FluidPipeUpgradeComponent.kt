package ic2_120.content.upgrade

import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidEjectorUpgrade
import ic2_120.content.item.FluidPullingUpgrade
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
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

object FluidPipeUpgradeComponent {
    private const val NBT_FILTER = "PipeFluidFilter"
    private const val NBT_DIRECTION = "PipeFluidDirection"

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
        var providerSide: Direction? = null
        var receiverSide: Direction? = null

        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (stack.isEmpty) continue
            when (stack.item) {
                is FluidEjectorUpgrade -> {
                    provider = true
                    if (providerFilter == null) providerFilter = readFilter(stack)
                    if (providerSide == null) providerSide = readDirection(stack)
                }
                is FluidPullingUpgrade -> {
                    receiver = true
                    if (receiverFilter == null) receiverFilter = readFilter(stack)
                    if (receiverSide == null) receiverSide = readDirection(stack)
                }
            }
        }

        machine.fluidPipeProviderEnabled = provider
        machine.fluidPipeReceiverEnabled = receiver
        machine.fluidPipeProviderFilter = providerFilter
        machine.fluidPipeReceiverFilter = receiverFilter
        machine.fluidPipeProviderSide = providerSide
        machine.fluidPipeReceiverSide = receiverSide
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

    fun readDirection(stack: ItemStack): Direction? {
        val nbt = stack.getCustomData() ?: return null
        val raw = nbt.getString(NBT_DIRECTION)
        if (raw.isNullOrBlank()) return null
        return Direction.byName(raw.lowercase())
    }

    fun writeDirection(stack: ItemStack, side: Direction?) {
        stack.editCustomData { nbt ->
            if (side == null) {
                nbt.remove(NBT_DIRECTION)
                return@editCustomData
            }
            nbt.putString(NBT_DIRECTION, side.name.lowercase())
        }
    }

    /**
     * 将储罐中的流体弹出到相邻方块。
     * @param tank 流体储罐
     * @param filter 流体过滤器，null 表示不过滤
     * @param configuredSide 弹出方向，null 表示所有方向
     * @param blockedFace 禁止弹出的面（通常是机器正面），null 表示不禁止任何面
     */
    fun ejectFluidToNeighbors(
        world: World,
        pos: BlockPos,
        tank: SingleVariantStorage<FluidVariant>,
        filter: Fluid?,
        configuredSide: Direction?,
        blockedFace: Direction? = null
    ) {
        if (tank.amount <= 0L || tank.variant.isBlank) return
        for (dir in Direction.values()) {
            if (blockedFace != null && dir == blockedFace) continue
            if (configuredSide != null && dir != configuredSide) continue
            val neighbor = FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue
            val resource = tank.variant
            if (filter != null && resource.fluid != filter) continue
            val maxPerTick = minOf(FluidConstants.BUCKET / 4, tank.amount)
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

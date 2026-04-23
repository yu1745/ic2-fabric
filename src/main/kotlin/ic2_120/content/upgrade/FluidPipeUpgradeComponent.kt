package ic2_120.content.upgrade

import ic2_120.content.item.FluidEjectorUpgrade
import ic2_120.content.item.FluidPullingUpgrade
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

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

    fun readDirection(stack: ItemStack): Direction? {
        val nbt = stack.nbt ?: return null
        val raw = nbt.getString(NBT_DIRECTION)
        if (raw.isNullOrBlank()) return null
        return Direction.byName(raw.lowercase())
    }

    fun writeDirection(stack: ItemStack, side: Direction?) {
        val nbt = stack.orCreateNbt
        if (side == null) {
            nbt.remove(NBT_DIRECTION)
            return
        }
        nbt.putString(NBT_DIRECTION, side.name.lowercase())
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
}

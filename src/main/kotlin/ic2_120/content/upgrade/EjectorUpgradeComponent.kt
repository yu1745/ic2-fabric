package ic2_120.content.upgrade

import ic2_120.content.item.AdvancedEjectorUpgrade
import ic2_120.content.item.EjectorUpgrade
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.block.entity.HopperBlockEntity
import net.minecraft.inventory.SidedInventory

object EjectorUpgradeComponent {
    private const val NBT_ITEM_FILTER = "PipeItemFilter"
    private const val NBT_DIRECTION = "PipeItemDirection"

    /** 统一入口：按升级槽扫描弹出升级，写回机器弹出配置。 */
    fun <T> apply(machine: T, upgradeSlotIndices: IntArray, outputSlotIndices: IntArray) where T : Inventory, T : IEjectorUpgradeSupport {
        apply(machine as Inventory, upgradeSlotIndices, outputSlotIndices, machine as Any)
    }

    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, outputSlotIndices: IntArray, machine: Any) {
        if (machine !is IEjectorUpgradeSupport) return

        var provider = false
        var filter: Item? = null
        var side: Direction? = null

        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (stack.isEmpty) continue
            when (stack.item) {
                is EjectorUpgrade, is AdvancedEjectorUpgrade -> {
                    provider = true
                    if (filter == null) filter = readFilter(stack)
                    if (side == null) side = readDirection(stack)
                }
            }
        }

        machine.itemEjectorEnabled = provider
        machine.itemEjectorFilter = filter
        machine.itemEjectorSide = side
    }

    /**
     * 仅从 outputSlotIndices 指定槽位弹出物品。
     */
    fun ejectFromOutputSlots(
        world: World,
        pos: BlockPos,
        inventory: Inventory,
        outputSlotIndices: IntArray,
        side: Direction?,
        filter: Item?
    ) {
        if (outputSlotIndices.isEmpty()) return
        val dirs = if (side != null) listOf(side) else Direction.values().toList()

        for (slotIndex in outputSlotIndices) {
            val stack = inventory.getStack(slotIndex)
            if (stack.isEmpty) continue
            if (filter != null && stack.item != filter) continue

            var remaining = stack.copy()
            for (dir in dirs) {
                val target = HopperBlockEntity.getInventoryAt(world, pos.offset(dir)) ?: continue
                remaining = insertIntoInventory(target, remaining, dir.opposite)
                if (remaining.isEmpty) break
            }
            inventory.setStack(slotIndex, remaining)
        }
    }

    private fun insertIntoInventory(inventory: Inventory, stack: ItemStack, fromSide: Direction): ItemStack {
        var remaining = stack.copy()
        for (slot in 0 until inventory.size()) {
            if (remaining.isEmpty) break
            if (!canInsertToSlot(inventory, slot, remaining, fromSide)) continue
            val existing = inventory.getStack(slot)
            if (existing.isEmpty) {
                val toInsert = minOf(remaining.count, inventory.maxCountPerStack)
                val inserted = remaining.copy()
                inserted.count = toInsert
                inventory.setStack(slot, inserted)
                remaining.decrement(toInsert)
            } else if (ItemStack.canCombine(existing, remaining)) {
                val room = minOf(existing.maxCount, inventory.maxCountPerStack) - existing.count
                if (room > 0) {
                    val move = minOf(room, remaining.count)
                    existing.increment(move)
                    remaining.decrement(move)
                }
            }
        }
        return remaining
    }

    private fun canInsertToSlot(inventory: Inventory, slot: Int, stack: ItemStack, fromSide: Direction): Boolean {
        if (!inventory.isValid(slot, stack)) return false
        if (inventory is SidedInventory && !inventory.canInsert(slot, stack, fromSide)) return false
        return true
    }

    fun readFilter(stack: ItemStack): Item? {
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

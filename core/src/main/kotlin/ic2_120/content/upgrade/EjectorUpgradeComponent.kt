package ic2_120.content.upgrade

import ic2_120.content.item.EjectorUpgrade
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

object EjectorUpgradeComponent {
    private const val NBT_ITEM_FILTER = "PipeItemFilter"
    private const val NBT_DIRECTION = "PipeItemDirection"
    private const val NBT_DIRECTIONS = "PipeItemDirections"

    private data class EjectorConfig(val filter: Item?, val sides: Set<Direction>)

    /**
     * 统一入口：扫描升级槽中的所有弹出升级，逐个独立弹出 outputSlotIndices 中的物品。
     * 每个弹出升级使用自己的过滤和方向配置。
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
                configs.add(EjectorConfig(readFilter(stack), readDirections(stack)))
            }
        }
        if (configs.isEmpty()) return

        for (config in configs) {
            val dirs = if (config.sides.isEmpty()) Direction.values().toList() else config.sides.toList()

            for (slotIndex in outputSlotIndices) {
                val stack = inventory.getStack(slotIndex)
                if (stack.isEmpty) continue
                if (config.filter != null && stack.item != config.filter) continue

                var remaining = stack.count
                val variant = ItemVariant.of(stack)

                for (dir in dirs) {
                    if (remaining <= 0) break
                    val target = ItemStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue
                    val tx = Transaction.openOuter()
                    val moved = target.insert(variant, remaining.toLong(), tx)
                    tx.commit()
                    remaining -= moved.toInt()
                }

                if (remaining <= 0) {
                    inventory.setStack(slotIndex, ItemStack.EMPTY)
                } else {
                    val newStack = stack.copy()
                    newStack.count = remaining
                    inventory.setStack(slotIndex, newStack)
                }
            }
        }
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
        return readDirections(stack).singleOrNull()
    }

    fun writeDirection(stack: ItemStack, side: Direction?) {
        writeDirections(stack, if (side == null) emptySet() else setOf(side))
    }

    /** 空集合表示任意方向；同时兼容旧版本的单方向 NBT。 */
    fun readDirections(stack: ItemStack): Set<Direction> {
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

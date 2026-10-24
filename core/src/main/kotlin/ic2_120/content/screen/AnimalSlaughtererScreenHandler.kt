package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.content.block.AnimalSlaughtererBlock
import ic2_120.content.block.machines.AnimalSlaughtererBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.AnimalSlaughtererSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(block = AnimalSlaughtererBlock::class, inventorySize = AnimalSlaughtererBlockEntity.INVENTORY_SIZE)
class AnimalSlaughtererScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(AnimalSlaughtererScreenHandler::class.type(), syncId) {

    val sync = AnimalSlaughtererSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkContainerSize(blockInventory, AnimalSlaughtererBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        for ((i, slotIndex) in AnimalSlaughtererBlockEntity.SLOT_CONTENT_INDICES.withIndex()) {
            addTrackedSlot(blockInventory, slotIndex, 48 + (i % 5) * 18, 17 + (i / 5) * 18)
        }
        for ((i, slotIndex) in AnimalSlaughtererBlockEntity.SLOT_UPGRADE_INDICES.withIndex()) {
            addTrackedSlot(blockInventory, slotIndex, 152, 8 + i * 18)
        }
        addTrackedSlot(blockInventory, AnimalSlaughtererBlockEntity.SLOT_DISCHARGING, 16, 53)
        addTrackedSlot(blockInventory, AnimalSlaughtererBlockEntity.SLOT_SHEARS, 16, 18)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    private fun addTrackedSlot(inventory: Container, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    // 守卫：仅机器槽可落此（玩家槽已被区间检查全覆盖）。若未来条件收窄使玩家槽落入，
                    // 直接拒绝而非向含源槽的玩家区间 insertItem（同实例自合并会复制物品）。
                    if (index >= PLAYER_INV_START) return ItemStack.EMPTY
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY)
            else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            world.getBlockState(pos).block is AnimalSlaughtererBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_CONTENT_INDEX_START = 0
        const val SLOT_CONTENT_INDEX_END = 14
        const val SLOT_UPGRADE_INDEX_START = 15
        const val SLOT_UPGRADE_INDEX_END = 18
        const val SLOT_DISCHARGING_INDEX = 19
        const val SLOT_SHEARS_INDEX = 20

        const val PLAYER_INV_START = 21
        const val HOTBAR_END = 57

    }
}

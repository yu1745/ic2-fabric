package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.block.machines.SolarDistillerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.SolarDistillerSync
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

@ModScreenHandler(block = SolarDistillerBlock::class, inventorySize = SolarDistillerBlockEntity.INVENTORY_SIZE)
class SolarDistillerScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(SolarDistillerScreenHandler::class.type(), syncId) {

    val sync = SolarDistillerSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkContainerSize(blockInventory, SolarDistillerBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_INPUT_WATER, 17, 27)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_OUTPUT_EMPTY, 17, 45)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_INPUT_CELL, 136, 64)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_OUTPUT_CELL, 136, 82)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_UPGRADE_0, 152, 8)
        addTrackedSlot(blockInventory, SolarDistillerBlockEntity.SLOT_UPGRADE_1, 152, 26)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 160))
        }
    }

    private fun addTrackedSlot(inventory: Container, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val slot = PredicateSlot(inventory, beSlotIndex, x, y, spec)
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot .hasItem()) return stack
        val inSlot = slot.stack
        stack = inSlot.copy()

        when (index) {
            SLOT_OUTPUT_EMPTY_INDEX, SLOT_OUTPUT_CELL_INDEX -> {
                if (!moveItemStackTo(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            SLOT_UPGRADE_0_INDEX, SLOT_UPGRADE_1_INDEX -> {
                if (!moveItemStackTo(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            in PLAYER_INV_START..HOTBAR_END -> {
                val storage = itemStorage ?: return ItemStack.EMPTY
                val moved = SlotMoveHelper.insertFromRoutes(
                    inSlot,
                    storage,
                    storage.insertRoutes,
                    beSlotToHandlerIndex,
                    slots
                )
                if (!moved) return ItemStack.EMPTY
            }
            else -> {
                // 守卫：仅机器槽可落此（玩家槽已被区间检查全覆盖）。若未来条件收窄使玩家槽落入，
                // 直接拒绝而非向含源槽的玩家区间 insertItem（同实例自合并会复制物品）。
                if (index >= PLAYER_INV_START) return ItemStack.EMPTY
                if (!moveItemStackTo(inSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
            }
        }

        if (inSlot.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        if (inSlot.count == stack.count) return ItemStack.EMPTY
        slot.onTake(player, inSlot)
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            world.getBlockState(pos).block is SolarDistillerBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_INPUT_WATER_INDEX = 0
        const val SLOT_OUTPUT_EMPTY_INDEX = 1
        const val SLOT_INPUT_CELL_INDEX = 2
        const val SLOT_OUTPUT_CELL_INDEX = 3
        const val SLOT_UPGRADE_0_INDEX = 4
        const val SLOT_UPGRADE_1_INDEX = 5
        const val PLAYER_INV_START = 6
        const val HOTBAR_END = 41

    }
}

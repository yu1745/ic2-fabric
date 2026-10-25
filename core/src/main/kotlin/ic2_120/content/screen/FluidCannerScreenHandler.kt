package ic2_120.content.screen
import ic2_120.compat.blockEntityOrNull

import ic2_120.compat.stack

import ic2_120.content.block.FluidCannerBlock
import ic2_120.content.block.machines.FluidCannerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.FluidCannerSync
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

@ModScreenHandler(block = FluidCannerBlock::class, inventorySize = FluidCannerBlockEntity.INVENTORY_SIZE)
class FluidCannerScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(FluidCannerScreenHandler::class.type(), syncId) {

    val sync = FluidCannerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.blockEntityOrNull() }
    }

    /** Maps BE slot index -> handler slot index for quickMove routing. */
    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(slot: Slot, beSlotIndex: Int) {
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    init {
        checkContainerSize(blockInventory, FluidCannerBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        addTrackedSlot(PredicateSlot(blockInventory, FluidCannerBlockEntity.SLOT_INPUT_FILLED, 44, 35, itemStorage?.deriveSlotSpec(FluidCannerBlockEntity.SLOT_INPUT_FILLED) ?: SlotSpec()), FluidCannerBlockEntity.SLOT_INPUT_FILLED)
        addTrackedSlot(PredicateSlot(blockInventory, FluidCannerBlockEntity.SLOT_INPUT_EMPTY, 44, 72, itemStorage?.deriveSlotSpec(FluidCannerBlockEntity.SLOT_INPUT_EMPTY) ?: SlotSpec()), FluidCannerBlockEntity.SLOT_INPUT_EMPTY)
        addTrackedSlot(PredicateSlot(blockInventory, FluidCannerBlockEntity.SLOT_OUTPUT, 117, 53, itemStorage?.deriveSlotSpec(FluidCannerBlockEntity.SLOT_OUTPUT) ?: SlotSpec(canInsert = { false }, canTake = { true })), FluidCannerBlockEntity.SLOT_OUTPUT)
        addTrackedSlot(PredicateSlot(blockInventory, FluidCannerBlockEntity.SLOT_DISCHARGING, 8, 53, itemStorage?.deriveSlotSpec(FluidCannerBlockEntity.SLOT_DISCHARGING) ?: SlotSpec()), FluidCannerBlockEntity.SLOT_DISCHARGING)

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                PredicateSlot(
                    blockInventory,
                    FluidCannerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    152,
                    26 + i * 18,
                    upgradeSlotSpec
                ),
                FluidCannerBlockEntity.SLOT_UPGRADE_INDICES[i]
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 160))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_OUTPUT_INDEX -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
                }
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
                }
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
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
            world.getBlockState(pos).block is FluidCannerBlock && player.distanceToSqr(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_INPUT_FILLED_INDEX = 0
        const val SLOT_INPUT_EMPTY_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44

    }
}

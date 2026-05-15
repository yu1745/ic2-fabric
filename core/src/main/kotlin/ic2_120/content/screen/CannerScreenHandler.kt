package ic2_120.content.screen

import ic2_120.content.block.CannerBlock
import ic2_120.content.block.machines.CannerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.CannerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = CannerBlock::class)
class CannerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(CannerScreenHandler::class.type(), syncId) {

    val sync = CannerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    /** Maps BE slot index -> handler slot index for quickMove routing. */
    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    /**
     * Add a tracked slot: registers it with the handler and records the BE->handler mapping.
     */
    private fun addTrackedSlot(slot: Slot, beSlotIndex: Int) {
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    init {
        checkSize(blockInventory, CannerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_CONTAINER, 0, 0, itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_CONTAINER) ?: SlotSpec()), CannerBlockEntity.SLOT_CONTAINER)
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_MATERIAL, 0, 0, itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_MATERIAL) ?: SlotSpec()), CannerBlockEntity.SLOT_MATERIAL)
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_OUTPUT, 0, 0, itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_OUTPUT) ?: SlotSpec(canInsert = { false }, canTake = { true })), CannerBlockEntity.SLOT_OUTPUT)
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_DISCHARGING, 0, 0, itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_DISCHARGING) ?: SlotSpec()), CannerBlockEntity.SLOT_DISCHARGING)

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                PredicateSlot(
                    blockInventory,
                    CannerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    0,
                    0,
                    upgradeSlotSpec
                ),
                CannerBlockEntity.SLOT_UPGRADE_INDICES[i]
            )
        }

        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_LEFT_EMPTY, 0, 0, itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_LEFT_EMPTY) ?: SlotSpec(canInsert = { false }, canTake = { true })), CannerBlockEntity.SLOT_LEFT_EMPTY)
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_RIGHT_INPUT, 0, 0, itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_RIGHT_INPUT) ?: SlotSpec()), CannerBlockEntity.SLOT_RIGHT_INPUT)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is CannerBlockEntity) {
                when (id) {
                    BUTTON_ID_MODE_CYCLE -> be.cycleMode()
                    BUTTON_ID_SWAP_TANKS -> {
                        val leftBefore = be.sync.leftFluidAmountMb
                        val rightBefore = be.sync.rightFluidAmountMb
                        val changed = be.swapTanks()
                        val leftAfter = be.sync.leftFluidAmountMb
                        val rightAfter = be.sync.rightFluidAmountMb
                        player.sendMessage(
                            Text.translatable(
                                if (changed) {
                                    "gui.ic2_120.canner.tanks_swapped"
                                } else {
                                    "gui.ic2_120.canner.tanks_swap_unchanged"
                                },
                                *if (changed) {
                                    arrayOf(leftBefore, leftAfter, rightBefore, rightAfter)
                                } else {
                                    arrayOf(leftAfter, rightAfter)
                                }
                            ),
                            true
                        )
                    }
                    else -> return@get
                }
            }
        }, true)
        return id == BUTTON_ID_MODE_CYCLE || id == BUTTON_ID_SWAP_TANKS
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_OUTPUT_INDEX || index == SLOT_LEFT_EMPTY_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val targets = buildRouteTargets()
                    val moved = SlotMoveHelper.insertIntoTargets(stackInSlot, targets)
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is CannerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    /**
     * Build ordered slot targets from itemStorage insert routes, falling back to empty
     * when itemStorage is null (client side).
     */
    private fun buildRouteTargets(): List<SlotTarget> {
        if (itemStorage == null) return emptyList()
        val routes = itemStorage.insertRoutes
        val targets = mutableListOf<SlotTarget>()
        for (route in routes) {
            for (beSlot in route.slotIndices) {
                val handlerIdx = beSlotToHandlerIndex[beSlot] ?: continue
                val spec = itemStorage.deriveSlotSpec(beSlot)
                targets.add(SlotTarget(slots[handlerIdx], spec))
            }
        }
        return targets
    }

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_CONTAINER_INDEX = 0
        const val SLOT_MATERIAL_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val SLOT_LEFT_EMPTY_INDEX = 8
        const val SLOT_RIGHT_INPUT_INDEX = 9
        const val PLAYER_INV_START = 10
        const val HOTBAR_END = 46
        const val BUTTON_ID_MODE_CYCLE = 0
        const val BUTTON_ID_SWAP_TANKS = 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(CannerBlockEntity.INVENTORY_SIZE)
            return CannerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

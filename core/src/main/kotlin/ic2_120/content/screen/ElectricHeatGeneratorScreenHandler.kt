package ic2_120.content.screen

import ic2_120.content.block.ElectricHeatGeneratorBlock
import ic2_120.content.block.machines.ElectricHeatGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = ElectricHeatGeneratorBlock::class, inventorySize = ElectricHeatGeneratorBlockEntity.SLOT_COUNT)
class ElectricHeatGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(ElectricHeatGeneratorScreenHandler::class.type(), syncId) {

    private val syncedView = SyncedDataView(propertyDelegate)
    private val heatFlow = HeatFlowSync(
        syncedView,
        object : HeatFlowSync.HeatProducer {
            override fun getLastGeneratedHeat(): Long = 0L
            override fun getLastOutputHeat(): Long = 0L
        }
    )
    val sync = ElectricHeatGeneratorSync(syncedView, heatFlow = heatFlow)

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, ElectricHeatGeneratorBlockEntity.SLOT_COUNT)
        addProperties(propertyDelegate)

        // 线圈槽：2行×5列，起始 (44, 26)，间距 18
        for (row in 0 until 2) {
            for (col in 0 until 5) {
                addTrackedSlot(blockInventory, row * 5 + col, 44 + col * 18, 26 + row * 18)
            }
        }
        // 放电槽
        addTrackedSlot(blockInventory, ElectricHeatGeneratorBlockEntity.SLOT_DISCHARGING, 8, 61)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 83 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 141))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int = 0, y: Int = 0) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
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
            world.getBlockState(pos).block is ElectricHeatGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_DISCHARGING_INDEX = ElectricHeatGeneratorBlockEntity.SLOT_DISCHARGING
        const val PLAYER_INV_START = ElectricHeatGeneratorBlockEntity.SLOT_COUNT
        const val HOTBAR_END = PLAYER_INV_START + 35

    }
}

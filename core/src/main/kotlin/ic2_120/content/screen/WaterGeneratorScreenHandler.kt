package ic2_120.content.screen

import ic2_120.content.block.WaterGeneratorBlock
import ic2_120.content.block.machines.WaterGeneratorBlockEntity
import ic2_120.content.item.isWaterFuel
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.WaterGeneratorSync
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

@ModScreenHandler(block = WaterGeneratorBlock::class, inventorySize = WaterGeneratorBlockEntity.INVENTORY_SIZE)
class WaterGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(WaterGeneratorScreenHandler::class.type(), syncId) {

    val sync = WaterGeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(slot: Slot, beSlotIndex: Int) {
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    init {
        checkSize(blockInventory, WaterGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 机器槽位（规则由 RoutedItemStorage 路由推导）
        val fuelSlotSpec = itemStorage?.deriveSlotSpec(WaterGeneratorBlockEntity.FUEL_SLOT)
            ?: SLOT_SPEC_FALLBACK_FUEL
        val emptyContainerSlotSpec = itemStorage?.deriveSlotSpec(WaterGeneratorBlockEntity.EMPTY_CONTAINER_SLOT)
            ?: SLOT_SPEC_FALLBACK_EMPTY_CONTAINER
        val batterySlotSpec = itemStorage?.deriveSlotSpec(WaterGeneratorBlockEntity.BATTERY_SLOT)
            ?: SLOT_SPEC_FALLBACK_BATTERY

        addTrackedSlot(PredicateSlot(blockInventory, WaterGeneratorBlockEntity.FUEL_SLOT, 38, 18, fuelSlotSpec), WaterGeneratorBlockEntity.FUEL_SLOT)
        addTrackedSlot(PredicateSlot(blockInventory, WaterGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, 38, 57, emptyContainerSlotSpec), WaterGeneratorBlockEntity.EMPTY_CONTAINER_SLOT)
        addTrackedSlot(PredicateSlot(blockInventory, WaterGeneratorBlockEntity.BATTERY_SLOT, 134, 38, batterySlotSpec), WaterGeneratorBlockEntity.BATTERY_SLOT)

        // 4 个升级槽
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                PredicateSlot(
                    blockInventory,
                    WaterGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i],
                    176,
                    8 + i * 18,
                    upgradeSlotSpec
                ),
                WaterGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i]
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 79 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 137))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_FUEL_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                index == SLOT_EMPTY_CONTAINER_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                index == SLOT_BATTERY_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
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
            world.getBlockState(pos).block is WaterGeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        const val SLOT_FUEL_INDEX = 0
        const val SLOT_EMPTY_CONTAINER_INDEX = 1
        const val SLOT_BATTERY_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        private val SLOT_SPEC_FALLBACK_FUEL = SlotSpec(
            maxItemCount = 64,
            canInsert = { stack -> stack.isWaterFuel() }
        )
        private val SLOT_SPEC_FALLBACK_EMPTY_CONTAINER = SlotSpec(
            maxItemCount = 64,
            canInsert = { false }
        )
        private val SLOT_SPEC_FALLBACK_BATTERY = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.canBeCharged() }
        )

    }
}

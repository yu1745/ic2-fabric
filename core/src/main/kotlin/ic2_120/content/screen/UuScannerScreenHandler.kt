package ic2_120.content.screen

import ic2_120.content.block.UuScannerBlock
import ic2_120.content.block.machines.UuScannerBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.UuScannerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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

@ModScreenHandler(block = UuScannerBlock::class)
class UuScannerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate
) : ScreenHandler(UuScannerScreenHandler::class.type(), syncId) {

    val sync = UuScannerSync(
        SyncedDataView(propertyDelegate),
        { null },
        { UuScannerSync.ENERGY_CAPACITY }
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, UuScannerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_INPUT, 0, 0, INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_DISCHARGING, 0, 0, BATTERY_SLOT_SPEC))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    UuScannerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    0, 0,
                    upgradeSlotSpec
                )
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index in MACHINE_SLOT_START..MACHINE_SLOT_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC),
                            SlotTarget(slots[SLOT_BATTERY_INDEX], BATTERY_SLOT_SPEC)
                        ) + (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is UuScannerBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        /** 此 Handler 使用的 GUI 尺寸 */
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE

        private val INPUT_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> !stack.isEmpty && stack.item !is IBatteryItem }
        )
        private val BATTERY_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )

        const val SLOT_INPUT_INDEX = 0
        const val SLOT_BATTERY_INDEX = 1
        const val SLOT_UPGRADE_INDEX_START = 2
        const val SLOT_UPGRADE_INDEX_END = SLOT_UPGRADE_INDEX_START + 3
        const val MACHINE_SLOT_START = SLOT_INPUT_INDEX
        const val MACHINE_SLOT_END = SLOT_UPGRADE_INDEX_END
        const val PLAYER_INV_START = MACHINE_SLOT_END + 1
        const val HOTBAR_END = PLAYER_INV_START + 36 - 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): UuScannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            return UuScannerScreenHandler(
                syncId,
                playerInventory,
                SimpleInventory(UuScannerBlockEntity.INVENTORY_SIZE),
                ScreenHandlerContext.create(playerInventory.player.world, pos),
                ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}

package ic2_120.content.screen

import ic2_120.content.block.TeleporterBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.TeleporterSync
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

@ModScreenHandler(block = TeleporterBlock::class)
class TeleporterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(TeleporterScreenHandler::class.type(), syncId) {

    val sync = TeleporterSync(SyncedDataView(propertyDelegate))

    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    private val upgradeSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IUpgradeItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, TeleporterBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, TeleporterBlockEntity.SLOT_DISCHARGING, BATTERY_X, BATTERY_Y, dischargingSlotSpec))
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    TeleporterBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i),
                    upgradeSlotSpec
                )
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id != BUTTON_ID_RANGE_DEC && id != BUTTON_ID_RANGE_INC) return false
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? TeleporterBlockEntity ?: return@get
            val next = if (id == BUTTON_ID_RANGE_INC) {
                TeleporterBlockEntity.TELEPORT_RANGE_MAX
            } else {
                TeleporterBlockEntity.TELEPORT_RANGE_MIN
            }
            be.setTeleportRange(next)
        }, true)
        return true
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index in 0..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                        SlotTarget(slots[it], upgradeSlotSpec)
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec)) + upgradeTargets
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
            world.getBlockState(pos).block is TeleporterBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val BATTERY_X = 80
        const val BATTERY_Y = 44
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142

        const val SLOT_DISCHARGING_INDEX = 0
        const val SLOT_UPGRADE_INDEX_START = 1
        const val SLOT_UPGRADE_INDEX_END = 4
        const val PLAYER_INV_START = 5
        const val HOTBAR_END = 41
        const val BUTTON_ID_RANGE_DEC = 0
        const val BUTTON_ID_RANGE_INC = 1

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): TeleporterScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(TeleporterBlockEntity.INVENTORY_SIZE)
            return TeleporterScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

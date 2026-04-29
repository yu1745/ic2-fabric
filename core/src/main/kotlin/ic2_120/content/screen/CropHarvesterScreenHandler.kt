package ic2_120.content.screen

import ic2_120.content.block.CropHarvesterBlock
import ic2_120.content.block.machines.CropHarvesterBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.CropHarvesterSync
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

@ModScreenHandler(block = CropHarvesterBlock::class)
class CropHarvesterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(CropHarvesterScreenHandler::class.type(), syncId) {

    val sync = CropHarvesterSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, CropHarvesterBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        for (slotIndex in CropHarvesterBlockEntity.SLOT_CONTENT_INDICES) {
            addSlot(PredicateSlot(blockInventory, slotIndex, 0, 0, OUTPUT_ONLY_SLOT_SPEC))
        }
        for (slotIndex in CropHarvesterBlockEntity.SLOT_UPGRADE_INDICES) {
            addSlot(PredicateSlot(blockInventory, slotIndex, 0, 0, upgradeSlotSpec))
        }
        addSlot(PredicateSlot(blockInventory, CropHarvesterBlockEntity.SLOT_DISCHARGING, 0, 0, dischargingSlotSpec))

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
        val slot = slots[index]
        if (!slot.hasStack()) return ItemStack.EMPTY

        val inSlot = slot.stack
        val original = inSlot.copy()

        when {
            index in SLOT_CONTENT_INDEX_START..SLOT_CONTENT_INDEX_END ||
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END ||
                index == SLOT_DISCHARGING_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            index in PLAYER_INV_START..HOTBAR_END -> {
                val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                    SlotTarget(slots[it], upgradeSlotSpec)
                }
                val moved = SlotMoveHelper.insertIntoTargets(
                    inSlot,
                    listOf(SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec)) + upgradeTargets
                )
                if (!moved) return ItemStack.EMPTY
            }
            else -> if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
        }

        if (inSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (inSlot.count == original.count) return ItemStack.EMPTY
        slot.onTakeItem(player, inSlot)
        return original
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is CropHarvesterBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_CONTENT_INDEX_START = 0
        const val SLOT_CONTENT_INDEX_END = 14
        const val SLOT_UPGRADE_INDEX_START = 15
        const val SLOT_UPGRADE_INDEX_END = 18
        const val SLOT_DISCHARGING_INDEX = 19

        const val PLAYER_INV_START = 20
        const val HOTBAR_END = 55

        private val OUTPUT_ONLY_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CropHarvesterScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(CropHarvesterBlockEntity.INVENTORY_SIZE)
            return CropHarvesterScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

package ic2_120.content.screen

import ic2_120.Ic2_120
import ic2_120.content.block.MatterGeneratorBlock
import ic2_120.content.block.machines.MatterGeneratorBlockEntity
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.MatterGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier

@ModScreenHandler(block = MatterGeneratorBlock::class)
class MatterGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(MatterGeneratorScreenHandler::class.type(), syncId) {

    val sync = MatterGeneratorSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, MatterGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_SCRAP, 0, 0, SCRAP_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_CONTAINER_INPUT, 0, 0, CONTAINER_INPUT_SPEC))
        addSlot(PredicateSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_CONTAINER_OUTPUT, 0, 0, CONTAINER_OUTPUT_SPEC))
        addSlot(PredicateSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_DISCHARGING, 0, 0, DISCHARGING_SLOT_SPEC))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    MatterGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i],
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
            when (index) {
                SLOT_CONTAINER_OUTPUT_INDEX, SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                else -> {
                    if (index in PLAYER_INV_START..HOTBAR_END) {
                        val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC),
                                SlotTarget(slots[SLOT_SCRAP_INDEX], SCRAP_SLOT_SPEC),
                                SlotTarget(slots[SLOT_CONTAINER_INPUT_INDEX], CONTAINER_INPUT_SPEC)
                            ) + upgradeTargets
                        )
                        if (!moved) return ItemStack.EMPTY
                    } else if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY
                    }
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
            world.getBlockState(pos).block is MatterGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        /** 此 Handler 使用的 GUI 尺寸 */
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE

        const val SLOT_SIZE = 18

        private val SCRAP_ITEM_ID = Identifier.of(Ic2_120.MOD_ID, "scrap")
        private val EMPTY_CELL_ID = Identifier.of(Ic2_120.MOD_ID, "empty_cell")
        private val FLUID_CELL_ID = Identifier.of(Ic2_120.MOD_ID, "fluid_cell")

        private val SCRAP_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> !stack.isEmpty && Registries.ITEM.getId(stack.item) == SCRAP_ITEM_ID }
        )
        private val CONTAINER_INPUT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item !is IBatteryItem && isFillableContainer(stack) }
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )
        private val CONTAINER_OUTPUT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        private fun isFillableContainer(stack: ItemStack): Boolean {
            if (stack.isEmpty) return false
            val itemId = Registries.ITEM.getId(stack.item)
            return itemId == EMPTY_CELL_ID ||
                stack.item == Items.BUCKET ||
                (itemId == FLUID_CELL_ID && stack.isFluidCellEmpty())
        }

        const val SLOT_SCRAP_INDEX = 0
        const val SLOT_CONTAINER_INPUT_INDEX = 1
        const val SLOT_CONTAINER_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MatterGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(MatterGeneratorBlockEntity.INVENTORY_SIZE)
            return MatterGeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

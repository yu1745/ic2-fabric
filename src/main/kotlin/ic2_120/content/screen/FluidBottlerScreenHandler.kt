package ic2_120.content.screen

import ic2_120.content.block.FluidBottlerBlock
import ic2_120.content.block.machines.FluidBottlerBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.FluidBottlerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = FluidBottlerBlock::class)
class FluidBottlerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(FluidBottlerScreenHandler::class.type(), syncId) {

    val sync = FluidBottlerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, FluidBottlerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, FluidBottlerBlockEntity.SLOT_INPUT_FILLED,
            INPUT_FILLED_SLOT_X, INPUT_FILLED_SLOT_Y, INPUT_FILLED_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, FluidBottlerBlockEntity.SLOT_INPUT_EMPTY,
            INPUT_EMPTY_SLOT_X, INPUT_EMPTY_SLOT_Y, INPUT_EMPTY_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, FluidBottlerBlockEntity.SLOT_OUTPUT,
            OUTPUT_SLOT_X, OUTPUT_SLOT_Y, OUTPUT_SLOT_SPEC))
        addSlot(PredicateSlot(
            blockInventory,
            FluidBottlerBlockEntity.SLOT_DISCHARGING,
            DISCHARGING_SLOT_X,
            DISCHARGING_SLOT_Y,
            DISCHARGING_SLOT_SPEC
        ))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    FluidBottlerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i),
                    upgradeSlotSpec
                )
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                SLOT_DISCHARGING_INDEX -> {
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
                                SlotTarget(slots[SLOT_INPUT_FILLED_INDEX], INPUT_FILLED_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_EMPTY_INDEX], INPUT_EMPTY_SLOT_SPEC)
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
            world.getBlockState(pos).block is FluidBottlerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val INPUT_FILLED_SLOT_X = 56
        const val INPUT_FILLED_SLOT_Y = 26
        const val INPUT_EMPTY_SLOT_X = 56
        const val INPUT_EMPTY_SLOT_Y = 44
        const val OUTPUT_SLOT_X = 116
        const val OUTPUT_SLOT_Y = 35
        const val DISCHARGING_SLOT_X = 56
        const val DISCHARGING_SLOT_Y = 62
        const val SLOT_SIZE = 18

        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        private val INPUT_FILLED_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item !is IBatteryItem && isFilledFluidContainer(stack)
            }
        )
        private val INPUT_EMPTY_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item !is IBatteryItem && isEmptyFluidContainer(stack)
            }
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        private fun isFilledFluidContainer(stack: ItemStack): Boolean {
            if (stack.isEmpty) return false
            if (stack.item == Items.WATER_BUCKET || stack.item == Items.LAVA_BUCKET) return true
            val ctx = ContainerItemContext.withConstant(stack)
            val storage = ctx.find(FluidStorage.ITEM) ?: return false
            for (view in storage) {
                if (view.amount >= FluidConstants.BUCKET && !view.resource.isBlank) return true
            }
            return false
        }

        private fun isEmptyFluidContainer(stack: ItemStack): Boolean {
            if (stack.isEmpty) return false
            if (stack.item == Items.BUCKET) return true
            val ctx = ContainerItemContext.withConstant(stack)
            val storage = ctx.find(FluidStorage.ITEM) ?: return false
            return storage.supportsInsertion()
        }

        const val SLOT_INPUT_FILLED_INDEX = 0
        const val SLOT_INPUT_EMPTY_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): FluidBottlerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(FluidBottlerBlockEntity.INVENTORY_SIZE)
            return FluidBottlerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

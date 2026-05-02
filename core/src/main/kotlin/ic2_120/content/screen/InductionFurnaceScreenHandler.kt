package ic2_120.content.screen

import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.block.machines.InductionFurnaceBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.upgrade.UpgradeItemRegistry
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = InductionFurnaceBlock::class, clientInventorySize = InductionFurnaceBlockEntity.INVENTORY_SIZE)
class InductionFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(InductionFurnaceScreenHandler::class.type(), syncId) {

    val sync = InductionFurnaceSync(SyncedDataView(propertyDelegate))

    private val inputSlotSpec0 = SlotSpec(canInsert = { stack -> stack.item !is IBatteryItem })
    private val inputSlotSpec1 = SlotSpec(canInsert = { stack -> stack.item !is IBatteryItem })
    private val outputSlotSpec0 = SlotSpec(canInsert = { false }, canTake = { true })
    private val outputSlotSpec1 = SlotSpec(canInsert = { false }, canTake = { true })
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        SlotSpec(
            canInsert = { stack ->
                if (stack.isEmpty || stack.item !is IUpgradeItem) return@SlotSpec false
                UpgradeItemRegistry.canAccept(
                    context.get({ world, pos -> world.getBlockEntity(pos) }, null),
                    stack.item
                )
            }
        )
    }

    init {
        checkSize(blockInventory, InductionFurnaceBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_INPUT_0, 0, 0, inputSlotSpec0))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_INPUT_1, 0, 0, inputSlotSpec1))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_OUTPUT_0, 0, 0, outputSlotSpec0))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_OUTPUT_1, 0, 0, outputSlotSpec1))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_DISCHARGING, 0, 0, dischargingSlotSpec))

        for (i in 0 until UPGRADE_SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    InductionFurnaceBlockEntity.SLOT_UPGRADE_INDICES[i],
                    0,
                    0,
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
                index == SLOT_OUTPUT_0_INDEX || index == SLOT_OUTPUT_1_INDEX -> {
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
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                        SlotTarget(slots[it], upgradeSlotSpec)
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec),
                            SlotTarget(slots[SLOT_INPUT_0_INDEX], inputSlotSpec0),
                            SlotTarget(slots[SLOT_INPUT_1_INDEX], inputSlotSpec1)
                        ) + upgradeTargets
                    )
                    if (!moved) {
                        return ItemStack.EMPTY
                    }
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
            world.getBlockState(pos).block is InductionFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        private const val UPGRADE_SLOT_COUNT = 4
        const val SLOT_SIZE = 18

        const val SLOT_INPUT_0_INDEX = 0
        const val SLOT_INPUT_1_INDEX = 1
        const val SLOT_OUTPUT_0_INDEX = 2
        const val SLOT_OUTPUT_1_INDEX = 3
        const val SLOT_DISCHARGING_INDEX = 4
        const val SLOT_UPGRADE_INDEX_START = 5
        const val SLOT_UPGRADE_INDEX_END = 8
        const val PLAYER_INV_START = 9
        const val HOTBAR_END = 45

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): InductionFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(InductionFurnaceBlockEntity.INVENTORY_SIZE)
            return InductionFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

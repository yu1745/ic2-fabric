package ic2_120.content.screen

import ic2_120.content.block.ExtractorBlock
import ic2_120.content.block.machines.ExtractorBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.ExtractorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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

@ModScreenHandler(block = ExtractorBlock::class)
class ExtractorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ExtractorScreenHandler::class.type(), syncId) {

    val sync = ExtractorSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val inputSlotSpec = SlotSpec(canInsert = { stack -> stack.item !is IBatteryItem && stack.item !is IUpgradeItem })
    private val outputSlotSpec = SlotSpec(canInsert = { false }, canTake = { true })
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, ExtractorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 机器槽位：Compose 屏幕会在客户端通过 SlotAnchor 回写真实坐标，这里仅放占位坐标。
        addSlot(
            PredicateSlot(
                blockInventory,
                ExtractorBlockEntity.SLOT_INPUT,
                0,
                0,
                inputSlotSpec
            )
        )
        addSlot(
            PredicateSlot(
                blockInventory,
                ExtractorBlockEntity.SLOT_OUTPUT,
                0,
                0,
                outputSlotSpec
            )
        )
        addSlot(
            PredicateSlot(
                blockInventory,
                ExtractorBlockEntity.SLOT_DISCHARGING,
                0,
                0,
                dischargingSlotSpec
            )
        )
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    ExtractorBlockEntity.SLOT_UPGRADE_INDICES[i],
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
                index == SLOT_OUTPUT_INDEX -> {
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
                    val dischargingTarget = SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec)
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(SlotTarget(slots[SLOT_INPUT_INDEX], inputSlotSpec), dischargingTarget) + upgradeTargets
                    )
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
            world.getBlockState(pos).block is ExtractorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_OUTPUT_INDEX = 1
        const val SLOT_DISCHARGING_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ExtractorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(ExtractorBlockEntity.INVENTORY_SIZE)
            return ExtractorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

package ic2_120.content.screen

import ic2_120.content.item.IBlockCuttingBlade
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.BlockCutterSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.BlockCutterBlock
import ic2_120.content.block.machines.BlockCutterBlockEntity
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
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = BlockCutterBlock::class)
class BlockCutterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(BlockCutterScreenHandler::class.type(), syncId) {

    val sync = BlockCutterSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, BlockCutterBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, BlockCutterBlockEntity.SLOT_INPUT, 0, 0, INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, BlockCutterBlockEntity.SLOT_DISCHARGING, 0, 0, DISCHARGING_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, BlockCutterBlockEntity.SLOT_OUTPUT, 0, 0, OUTPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, BlockCutterBlockEntity.SLOT_BLADE, 0, 0, BLADE_SLOT_SPEC))

        // 5: 升级插件槽 x4
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    BlockCutterBlockEntity.SLOT_UPGRADE_INDICES[i],
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
            when (index) {
                SLOT_OUTPUT_INDEX, SLOT_INPUT_INDEX, SLOT_BLADE_INDEX -> {
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
                                SlotTarget(slots[SLOT_BLADE_INDEX], BLADE_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC),
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC)
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
            world.getBlockState(pos).block is BlockCutterBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        private val BLADE_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBlockCuttingBlade }
        )
        private val INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item !is IBatteryItem && stack.item !is IUpgradeItem && stack.item !is IBlockCuttingBlade }
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        const val SLOT_INPUT_INDEX = 0
        const val SLOT_DISCHARGING_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_BLADE_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): BlockCutterScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(BlockCutterBlockEntity.INVENTORY_SIZE)
            return BlockCutterScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

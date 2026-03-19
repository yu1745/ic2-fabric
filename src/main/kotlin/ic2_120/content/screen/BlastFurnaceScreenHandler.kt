package ic2_120.content.screen

import ic2_120.content.block.BlastFurnaceBlock
import ic2_120.content.block.machines.BlastFurnaceBlockEntity
import ic2_120.content.item.AirCell
import ic2_120.content.recipes.BlastFurnaceRecipes
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.BlastFurnaceSync
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

@ModScreenHandler(block = BlastFurnaceBlock::class)
class BlastFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(BlastFurnaceScreenHandler::class.type(), syncId) {

    val sync = BlastFurnaceSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, BlastFurnaceBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, BlastFurnaceBlockEntity.SLOT_INPUT,
            INPUT_SLOT_X, INPUT_SLOT_Y, INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, BlastFurnaceBlockEntity.SLOT_AIR_INPUT,
            AIR_INPUT_SLOT_X, AIR_INPUT_SLOT_Y, AIR_INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, BlastFurnaceBlockEntity.SLOT_OUTPUT_STEEL,
            OUTPUT_STEEL_SLOT_X, OUTPUT_STEEL_SLOT_Y, OUTPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, BlastFurnaceBlockEntity.SLOT_OUTPUT_SLAG,
            OUTPUT_SLAG_SLOT_X, OUTPUT_SLAG_SLOT_Y, OUTPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, BlastFurnaceBlockEntity.SLOT_OUTPUT_EMPTY,
            EMPTY_OUTPUT_SLOT_X, EMPTY_OUTPUT_SLOT_Y, EMPTY_OUTPUT_SLOT_SPEC))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(PredicateSlot(
                blockInventory,
                BlastFurnaceBlockEntity.SLOT_UPGRADE_INDICES[i],
                UpgradeSlotLayout.SLOT_X,
                UpgradeSlotLayout.slotY(i),
                upgradeSlotSpec
            ))
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
                in SLOT_OUTPUT_STEEL_INDEX..SLOT_OUTPUT_EMPTY_INDEX -> {
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
                                SlotTarget(slots[SLOT_AIR_INPUT_INDEX], AIR_INPUT_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC)
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
            world.getBlockState(pos).block is BlastFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val INPUT_SLOT_X = 56
        const val INPUT_SLOT_Y = 35
        const val AIR_INPUT_SLOT_X = 56
        const val AIR_INPUT_SLOT_Y = 53
        const val OUTPUT_STEEL_SLOT_X = 116
        const val OUTPUT_STEEL_SLOT_Y = 35
        const val OUTPUT_SLAG_SLOT_X = 116
        const val OUTPUT_SLAG_SLOT_Y = 53
        const val EMPTY_OUTPUT_SLOT_X = 56
        const val EMPTY_OUTPUT_SLOT_Y = 71
        const val SLOT_SIZE = 18
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        private val INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> BlastFurnaceRecipes.isValidInput(stack) }
        )
        private val AIR_INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item is AirCell }
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })
        private val EMPTY_OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })

        const val SLOT_INPUT_INDEX = 0
        const val SLOT_AIR_INPUT_INDEX = 1
        const val SLOT_OUTPUT_STEEL_INDEX = 2
        const val SLOT_OUTPUT_SLAG_INDEX = 3
        const val SLOT_OUTPUT_EMPTY_INDEX = 4
        const val SLOT_UPGRADE_INDEX_START = 5
        const val SLOT_UPGRADE_INDEX_END = 8
        const val PLAYER_INV_START = 9
        const val HOTBAR_END = 45

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): BlastFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(BlastFurnaceBlockEntity.INVENTORY_SIZE)
            return BlastFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

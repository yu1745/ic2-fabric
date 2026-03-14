package ic2_120.content.screen

import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.block.machines.SolarDistillerBlockEntity
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.WaterCell
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.isWaterFuel
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier

@ModScreenHandler(block = SolarDistillerBlock::class)
class SolarDistillerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(SolarDistillerScreenHandler::class), syncId) {

    val sync = SolarDistillerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, SolarDistillerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, SolarDistillerBlockEntity.SLOT_INPUT_WATER, INPUT_WATER_X, TOP_Y, INPUT_WATER_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SolarDistillerBlockEntity.SLOT_OUTPUT_EMPTY, INPUT_WATER_X, TOP_Y + SLOT_SIZE, OUTPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SolarDistillerBlockEntity.SLOT_INPUT_CELL, OUTPUT_CELL_X, TOP_Y, INPUT_CELL_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SolarDistillerBlockEntity.SLOT_OUTPUT_CELL, OUTPUT_CELL_X, TOP_Y + SLOT_SIZE, OUTPUT_SLOT_SPEC))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    SolarDistillerBlockEntity.SLOT_UPGRADE_INDICES[i],
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

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasStack()) return stack
        val inSlot = slot.stack
        stack = inSlot.copy()

        when (index) {
            SLOT_OUTPUT_EMPTY_INDEX, SLOT_OUTPUT_CELL_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            in SLOT_UPGRADE_START..SLOT_UPGRADE_END -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            in PLAYER_INV_START..HOTBAR_END -> {
                val upgradeTargets = (SLOT_UPGRADE_START..SLOT_UPGRADE_END).map { SlotTarget(slots[it], upgradeSlotSpec) }
                val moved = SlotMoveHelper.insertIntoTargets(
                    inSlot,
                    listOf(
                        SlotTarget(slots[SLOT_INPUT_WATER_INDEX], INPUT_WATER_SLOT_SPEC),
                        SlotTarget(slots[SLOT_INPUT_CELL_INDEX], INPUT_CELL_SLOT_SPEC)
                    ) + upgradeTargets
                )
                if (!moved) return ItemStack.EMPTY
            }
            else -> if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
        }

        if (inSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (inSlot.count == stack.count) return ItemStack.EMPTY
        slot.onTakeItem(player, inSlot)
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is SolarDistillerBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        const val INPUT_WATER_X = 26
        const val OUTPUT_CELL_X = 122
        const val TOP_Y = 35
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        const val SLOT_INPUT_WATER_INDEX = 0
        const val SLOT_OUTPUT_EMPTY_INDEX = 1
        const val SLOT_INPUT_CELL_INDEX = 2
        const val SLOT_OUTPUT_CELL_INDEX = 3
        const val SLOT_UPGRADE_START = 4
        const val SLOT_UPGRADE_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 43

        private val INPUT_WATER_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item == Items.WATER_BUCKET || stack.item is WaterCell ||
                    (stack.item is FluidCellItem && stack.isWaterFuel())
            }
        )

        private val INPUT_CELL_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                val emptyCell = Registries.ITEM.get(Identifier("ic2_120", "empty_cell"))
                stack.item == emptyCell || (stack.item is FluidCellItem && stack.isFluidCellEmpty())
            }
        )

        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SolarDistillerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(SolarDistillerBlockEntity.INVENTORY_SIZE)
            return SolarDistillerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

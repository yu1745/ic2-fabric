package ic2_120.content.screen

import ic2_120.content.block.ReplicatorBlock
import ic2_120.content.block.machines.ReplicatorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.ReplicatorSync
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
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos

@ModScreenHandler(block = ReplicatorBlock::class)
class ReplicatorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    val blockPos: BlockPos,
    private val context: ScreenHandlerContext,
    propertyDelegate: net.minecraft.screen.PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(ReplicatorScreenHandler::class.type(), syncId) {

    val sync = ReplicatorSync(
        SyncedDataView(propertyDelegate),
        { null },
        { ReplicatorSync.ENERGY_CAPACITY }
    )

    private val upgradeSlotSpec: ic2_120.content.screen.slot.SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, ReplicatorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // All slot coordinates are anchored by the client screen at render time.
        addTrackedSlot(PredicateSlot(blockInventory, ReplicatorBlockEntity.SLOT_OUTPUT, 0, 0, deriveSpec(ReplicatorBlockEntity.SLOT_OUTPUT)))
        addTrackedSlot(PredicateSlot(blockInventory, ReplicatorBlockEntity.SLOT_CONTAINER_INPUT, 0, 0, deriveSpec(ReplicatorBlockEntity.SLOT_CONTAINER_INPUT)))
        addTrackedSlot(PredicateSlot(blockInventory, ReplicatorBlockEntity.SLOT_CONTAINER_OUTPUT, 0, 0, deriveSpec(ReplicatorBlockEntity.SLOT_CONTAINER_OUTPUT)))
        addTrackedSlot(PredicateSlot(blockInventory, ReplicatorBlockEntity.SLOT_DISCHARGING, 0, 0, deriveSpec(ReplicatorBlockEntity.SLOT_DISCHARGING)))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                PredicateSlot(
                    blockInventory,
                    ReplicatorBlockEntity.SLOT_UPGRADE_INDICES[i],
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

    private fun addTrackedSlot(slot: PredicateSlot): PredicateSlot {
        beSlotToHandlerIndex[slot.index] = slots.size
        addSlot(slot)
        return slot
    }

    private fun deriveSpec(beSlot: Int) =
        itemStorage?.deriveSlotSpec(beSlot) ?: SlotSpec()

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? ReplicatorBlockEntity ?: return@get false
            when {
                id == BUTTON_MODE_TOGGLE -> {
                    be.toggleMode()
                    true
                }
                id >= BUTTON_SELECT_BASE -> be.selectTemplate(id - BUTTON_SELECT_BASE)
                else -> false
            }
        }, false)
        return true
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_CONTAINER_OUTPUT_INDEX || index == SLOT_OUTPUT_INDEX || index == SLOT_BATTERY_INDEX || index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                        SlotTarget(slots[it], upgradeSlotSpec)
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SLOT_BATTERY_INDEX], deriveSpec(ReplicatorBlockEntity.SLOT_DISCHARGING)),
                            SlotTarget(slots[SLOT_CONTAINER_INPUT_INDEX], deriveSpec(ReplicatorBlockEntity.SLOT_CONTAINER_INPUT))
                        ) + upgradeTargets
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
            world.getBlockState(pos).block is ReplicatorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_OUTPUT_INDEX = 0
        const val SLOT_CONTAINER_INPUT_INDEX = 1
        const val SLOT_CONTAINER_OUTPUT_INDEX = 2
        const val SLOT_BATTERY_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = SLOT_UPGRADE_INDEX_START + 3
        const val MACHINE_SLOT_START = SLOT_OUTPUT_INDEX
        const val MACHINE_SLOT_END = SLOT_UPGRADE_INDEX_END
        const val PLAYER_INV_START = MACHINE_SLOT_END + 1
        const val HOTBAR_END = PLAYER_INV_START + 36 - 1

        const val BUTTON_MODE_TOGGLE = 1
        const val BUTTON_SELECT_BASE = 1000

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ReplicatorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            return ReplicatorScreenHandler(
                syncId,
                playerInventory,
                SimpleInventory(ReplicatorBlockEntity.INVENTORY_SIZE),
                pos,
                ScreenHandlerContext.create(playerInventory.player.world, pos),
                ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}

package ic2_120.content.screen

import ic2_120.content.block.machines.BaseMinerBlockEntity
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.MinerSync
import ic2_120.content.syncs.SyncedDataView
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

@ModScreenHandler(names = ["miner", "advanced_miner"])
class MinerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null,
    val isAdvanced: Boolean = false
) : ScreenHandler(MinerScreenHandler::class.type(), syncId) {

    val sync = MinerSync(
        SyncedDataView(propertyDelegate),
        { null },
        { MinerSync.BASE_ENERGY_CAPACITY }
    )

    private val upgradeSlotSpec: ic2_120.content.screen.slot.SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, BaseMinerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_SCANNER, 0, 0, deriveSpec(BaseMinerBlockEntity.SLOT_SCANNER)))
        addTrackedSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_DRILL, 0, 0, deriveSpec(BaseMinerBlockEntity.SLOT_DRILL)))
        addTrackedSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_DISCHARGING, 0, 0, deriveSpec(BaseMinerBlockEntity.SLOT_DISCHARGING)))
        addTrackedSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_PIPE, 0, 0, deriveSpec(BaseMinerBlockEntity.SLOT_PIPE)))

        var idx = BaseMinerBlockEntity.SLOT_FILTER_START
        repeat(BaseMinerBlockEntity.FILTER_SLOT_COUNT) {
            addTrackedSlot(PredicateSlot(blockInventory, idx++, 0, 0, deriveSpec(idx - 1)))
        }

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                PredicateSlot(
                    blockInventory,
                    BaseMinerBlockEntity.SLOT_UPGRADE_INDICES[i],
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

    private fun addTrackedSlot(slot: PredicateSlot): PredicateSlot {
        beSlotToHandlerIndex[slot.index] = slots.size
        addSlot(slot)
        return slot
    }

    private fun deriveSpec(beSlot: Int) =
        itemStorage?.deriveSlotSpec(beSlot) ?: SlotSpec()

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? BaseMinerBlockEntity ?: return@get false
            when (id) {
                BUTTON_TOGGLE_MODE -> if (isAdvanced) be.toggleMode()
                BUTTON_TOGGLE_SILK -> if (isAdvanced) be.toggleSilkTouch()
                BUTTON_RESTART -> be.restartScan()
                BUTTON_RECOVER_PIPES -> be.startPipeRecovery()
                else -> return@get false
            }
            true
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
                index in MACHINE_SLOT_START..MACHINE_SLOT_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                        SlotTarget(slots[it], upgradeSlotSpec)
                    }
                    val filterTargets = (SLOT_FILTER_INDEX_START..SLOT_FILTER_INDEX_END).map {
                        SlotTarget(slots[it], deriveSpec(BaseMinerBlockEntity.SLOT_FILTER_START + it - SLOT_FILTER_INDEX_START))
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SLOT_SCANNER_INDEX], deriveSpec(BaseMinerBlockEntity.SLOT_SCANNER)),
                            SlotTarget(slots[SLOT_DRILL_INDEX], deriveSpec(BaseMinerBlockEntity.SLOT_DRILL)),
                            SlotTarget(slots[SLOT_BATTERY_INDEX], deriveSpec(BaseMinerBlockEntity.SLOT_DISCHARGING)),
                            SlotTarget(slots[SLOT_PIPE_INDEX], deriveSpec(BaseMinerBlockEntity.SLOT_PIPE))
                        ) + upgradeTargets + filterTargets
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
            world.getBlockState(pos).block is BaseMinerBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SCANNER_INDEX = 0
        const val SLOT_DRILL_INDEX = 1
        const val SLOT_BATTERY_INDEX = 2
        const val SLOT_PIPE_INDEX = 3
        const val SLOT_FILTER_INDEX_START = 4
        const val SLOT_FILTER_INDEX_END = SLOT_FILTER_INDEX_START + BaseMinerBlockEntity.FILTER_SLOT_COUNT - 1
        const val SLOT_UPGRADE_INDEX_START = SLOT_FILTER_INDEX_END + 1
        const val SLOT_UPGRADE_INDEX_END = SLOT_UPGRADE_INDEX_START + 3
        const val MACHINE_SLOT_START = SLOT_SCANNER_INDEX
        const val MACHINE_SLOT_END = SLOT_UPGRADE_INDEX_END
        const val PLAYER_INV_START = MACHINE_SLOT_END + 1
        const val HOTBAR_END = PLAYER_INV_START + 36 - 1

        const val BUTTON_TOGGLE_MODE = 0
        const val BUTTON_TOGGLE_SILK = 1
        const val BUTTON_RESTART = 2
        const val BUTTON_RECOVER_PIPES = 3

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MinerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val isAdvanced = buf.readBoolean()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(BaseMinerBlockEntity.INVENTORY_SIZE)
            return MinerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount), isAdvanced = isAdvanced)
        }
    }
}

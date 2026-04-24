package ic2_120.content.screen

import ic2_120.content.block.machines.AdvancedMinerBlockEntity
import ic2_120.content.block.machines.BaseMinerBlockEntity
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.item.AdvancedScannerItem
import ic2_120.content.item.DiamondDrill
import ic2_120.content.item.Drill
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.OdScannerItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.MinerSync
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
import ic2_120.content.block.MiningPipeBlock
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.item

@ModScreenHandler(names = ["miner", "advanced_miner"])
class MinerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    val isAdvanced: Boolean = false
) : ScreenHandler(MinerScreenHandler::class.type(), syncId) {

    val sync = MinerSync(
        SyncedDataView(propertyDelegate),
        { null },
        { MinerSync.BASE_ENERGY_CAPACITY }
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val scannerSlotSpec = SlotSpec(canInsert = { stack ->
        when (stack.item) {
            is OdScannerItem -> true
            is AdvancedScannerItem -> context.get({ world, pos -> world.getBlockEntity(pos) is AdvancedMinerBlockEntity }, false)
            else -> false
        }
    }, maxItemCount = 1)

    private val drillSlotSpec = SlotSpec(canInsert = { stack ->
        stack.item is Drill || stack.item is DiamondDrill || stack.item is IridiumDrill
    }, maxItemCount = 1)

    private val batterySlotSpec = SlotSpec(canInsert = { stack -> stack.item is IBatteryItem }, maxItemCount = 1)
    private val pipeSlotSpec = SlotSpec(canInsert = { stack -> stack.item === MiningPipeBlock::class.item() }, maxItemCount = 1024)
    private val filterSlotSpec = SlotSpec(canInsert = { stack ->
        stack.item !== MiningPipeBlock::class.item()
    })

    init {
        checkSize(blockInventory, BaseMinerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_SCANNER, 0, 0, scannerSlotSpec))
        addSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_DRILL, 0, 0, drillSlotSpec))
        addSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_DISCHARGING, 0, 0, batterySlotSpec))
        addSlot(PredicateSlot(blockInventory, BaseMinerBlockEntity.SLOT_PIPE, 0, 0, pipeSlotSpec))

        var idx = BaseMinerBlockEntity.SLOT_FILTER_START
        repeat(BaseMinerBlockEntity.FILTER_SLOT_COUNT) {
            addSlot(PredicateSlot(blockInventory, idx++, 0, 0, filterSlotSpec))
        }

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
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

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? BaseMinerBlockEntity ?: return@get false
            when (id) {
                BUTTON_TOGGLE_MODE -> if (isAdvanced) be.toggleMode()
                BUTTON_TOGGLE_SILK -> if (isAdvanced) be.toggleSilkTouch()
                BUTTON_RESTART -> be.restartScan()
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
                        SlotTarget(slots[it], filterSlotSpec)
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SLOT_SCANNER_INDEX], scannerSlotSpec),
                            SlotTarget(slots[SLOT_DRILL_INDEX], drillSlotSpec),
                            SlotTarget(slots[SLOT_BATTERY_INDEX], batterySlotSpec),
                            SlotTarget(slots[SLOT_PIPE_INDEX], pipeSlotSpec)
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

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MinerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val isAdvanced = buf.readBoolean()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(BaseMinerBlockEntity.INVENTORY_SIZE)
            return MinerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount), isAdvanced)
        }
    }
}

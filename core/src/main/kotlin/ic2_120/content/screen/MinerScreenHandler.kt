package ic2_120.content.screen

import ic2_120.content.block.machines.BaseMinerBlockEntity
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
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
import net.minecraft.screen.slot.SlotActionType
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

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, BaseMinerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        if (isAdvanced) {
            // 高级采矿机：无钻头槽
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_SCANNER, 8, 26)
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_PIPE, 8, 44, 1024)
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_DISCHARGING, 8, 80)

            // 过滤槽 3×5（仅玩家可交互放置，每格限制1个）
            for (i in 0 until 15) {
                val beIndex = BaseMinerBlockEntity.SLOT_ITEM_START + i
                val spec = SlotSpec(
                    maxItemCount = 1,
                    canInsert = { stack -> stack.item is net.minecraft.item.BlockItem },
                    canTake = { true }
                )
                beSlotToHandlerIndex[beIndex] = slots.size
                addSlot(PredicateSlot(blockInventory, beIndex, 36 + (i % 5) * 18, 44 + (i / 5) * 18, spec))
            }

            // 升级槽 4个
            for (i in 0 until 4) {
                addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    152, 26 + i * 18)
            }

            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 121 + row * 18))
                }
            }
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col, 8 + col * 18, 179))
            }
        } else {
            // 普通采矿机
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_DRILL, 8, 21)
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_PIPE, 8, 39, 1024)
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_SCANNER, 8, 57)

            // 物品槽 5×3，起始 (32, 21)
            for (i in 0 until 15) {
                addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_ITEM_START + i,
                    32 + (i % 5) * 18, 21 + (i / 5) * 18)
            }

            // 升级槽 ×3，竖向排列
            for (i in 0 until 3) {
                addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_NORMAL_UPGRADE_INDICES[i],
                    152, 21 + i * 18)
            }
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_DISCHARGING, 128, 57)

            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 83 + row * 18))
                }
            }
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col, 8 + col * 18, 141))
            }
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int, maxItemCount: Int) {
        val base = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val spec = base.copy(maxItemCount = maxItemCount)
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        val slot = PredicateSlot(inventory, beSlotIndex, x, y, spec)
        addSlot(slot)
    }

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
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot == BaseMinerBlockEntity.SLOT_PIPE -> {
                    val miner = getMinerBlockEntity() ?: return ItemStack.EMPTY
                    val moved = miner.takePipes(stackInSlot.maxCount)
                    if (moved.isEmpty) return ItemStack.EMPTY
                    stack = moved.copy()
                    if (!insertItem(moved, playerSlotStart, slots.size, true)) {
                        miner.insertPipesFromStack(stack, stack.count)
                        return ItemStack.EMPTY
                    }
                    if (!moved.isEmpty) miner.insertPipesFromStack(moved, moved.count)
                    slot.onQuickTransfer(moved, stack)
                }
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, playerSlotStart, slots.size, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index >= playerSlotStart -> {
                    val miner = getMinerBlockEntity()
                    if (miner != null && miner.insertPipesFromStack(stackInSlot) > 0) {
                        // Pipe insertion bypasses vanilla merge limits and uses the miner's real 1024-slot capacity.
                    } else {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                    }
                }
                else -> {
                    if (!insertItem(stackInSlot, playerSlotStart, slots.size, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        val pipeSlotIndex = beSlotToHandlerIndex[BaseMinerBlockEntity.SLOT_PIPE]
        if (slotIndex == pipeSlotIndex && actionType == SlotActionType.PICKUP) {
            val miner = getMinerBlockEntity()
            if (miner != null) {
                val cursor = cursorStack
                if (cursor.isEmpty) {
                    val takeAmount = if (button == 1) 1 else cursor.maxCount
                    val taken = miner.takePipes(takeAmount)
                    if (!taken.isEmpty) {
                        cursorStack = taken
                        sendContentUpdates()
                        return
                    }
                } else {
                    val insertAmount = if (button == 1) 1 else cursor.count
                    if (miner.insertPipesFromStack(cursor, insertAmount) > 0) {
                        sendContentUpdates()
                        return
                    }
                }
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun getMinerBlockEntity(): BaseMinerBlockEntity? = try {
        context.get({ world, pos ->
            if (world.isClient) null else world.getBlockEntity(pos) as? BaseMinerBlockEntity
        }, null)
    } catch (_: NullPointerException) {
        null
    }

    private val playerSlotStart: Int
        get() = if (isAdvanced) 22 else 22

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is BaseMinerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
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

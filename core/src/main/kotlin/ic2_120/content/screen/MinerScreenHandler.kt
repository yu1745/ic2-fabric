package ic2_120.content.screen

import ic2_120.compat.world

import ic2_120.compat.stack

import ic2_120.compat.isClient

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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.ClickType
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(names = ["miner", "advanced_miner"])
class MinerScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null,
    val isAdvanced: Boolean = false
) : AbstractContainerMenu(MinerScreenHandler::class.type(), syncId) {

    val sync = MinerSync(
        SyncedDataView(propertyDelegate),
        { null },
        { if (isAdvanced) MinerSync.ADVANCED_CAPACITY else MinerSync.NORMAL_CAPACITY },
        baseCapacity = if (isAdvanced) MinerSync.ADVANCED_CAPACITY else MinerSync.NORMAL_CAPACITY
    )

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkContainerSize(blockInventory, BaseMinerBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        if (isAdvanced) {
            // 高级采矿机：无钻头槽
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_SCANNER, 8, 26)
            addTrackedSlot(blockInventory, BaseMinerBlockEntity.SLOT_DISCHARGING, 8, 80)

            // 过滤槽 3×5（仅玩家可交互放置，每格限制1个）
            for (i in 0 until 15) {
                val beIndex = BaseMinerBlockEntity.SLOT_ITEM_START + i
                val spec = SlotSpec(
                    maxItemCount = 1,
                    canInsert = { stack -> !stack.isEmpty },
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

    private fun addTrackedSlot(inventory: Container, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    private fun addTrackedSlot(inventory: Container, beSlotIndex: Int, x: Int, y: Int, maxItemCount: Int) {
        val base = itemStorage?.deriveSlotSpec(beSlotIndex) ?: SlotSpec()
        val spec = base.copy(maxItemCount = maxItemCount)
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        val slot = PredicateSlot(inventory, beSlotIndex, x, y, spec)
        addSlot(slot)
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        context.evaluate({ world, pos ->
            val be = world.getBlockEntity(pos) as? BaseMinerBlockEntity ?: return@evaluate false
            when (id) {
                BUTTON_TOGGLE_MODE -> if (isAdvanced) be.toggleMode()
                BUTTON_TOGGLE_SILK -> if (isAdvanced) be.toggleSilkTouch()
                BUTTON_RESTART -> be.restartScan()
                BUTTON_RECOVER_PIPES -> be.startPipeRecovery()
                else -> return@evaluate false
            }
            true
        }, false)
        return true
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot == BaseMinerBlockEntity.SLOT_PIPE -> {
                    val miner = getMinerBlockEntity() ?: return ItemStack.EMPTY
                    val moved = miner.takePipes(stackInSlot.maxStackSize)
                    if (moved.isEmpty) return ItemStack.EMPTY
                    stack = moved.copy()
                    if (!moveItemStackTo(moved, playerSlotStart, slots.size, true)) {
                        miner.insertPipesFromStack(stack, stack.count)
                        return ItemStack.EMPTY
                    }
                    if (!moved.isEmpty) miner.insertPipesFromStack(moved, moved.count)
                    slot.onTake(player, moved)
                }
                beSlot >= 0 -> {
                    if (!moveItemStackTo(stackInSlot, playerSlotStart, slots.size, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
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
                    if (!moveItemStackTo(stackInSlot, playerSlotStart, slots.size, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun clicked(slotIndex: Int, button: Int, actionType: ClickType, player: Player) {
        val pipeSlotIndex = beSlotToHandlerIndex[BaseMinerBlockEntity.SLOT_PIPE]
        if (slotIndex == pipeSlotIndex && actionType == ClickType.PICKUP) {
            val miner = getMinerBlockEntity()
            if (miner != null) {
                val cursor = carried
                if (cursor.isEmpty) {
                    val takeAmount = if (button == 1) 1 else cursor.maxStackSize
                    val taken = miner.takePipes(takeAmount)
                    if (!taken.isEmpty) {
                        carried = taken
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
        super.clicked(slotIndex, button, actionType, player)
    }

    private fun getMinerBlockEntity(): BaseMinerBlockEntity? = try {
        context.evaluate({ world, pos ->
            if (world.isClient) null else world.getBlockEntity(pos) as? BaseMinerBlockEntity
        }, null)
    } catch (_: NullPointerException) {
        null
    }

    private val playerSlotStart: Int
        get() = if (isAdvanced) 21 else 22

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            world.getBlockState(pos).block is BaseMinerBlock && player.distanceToSqr(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val BUTTON_TOGGLE_MODE = 0
        const val BUTTON_TOGGLE_SILK = 1
        const val BUTTON_RESTART = 2
        const val BUTTON_RECOVER_PIPES = 3

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): MinerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val isAdvanced = buf.readBoolean()
            val context = ContainerLevelAccess.create(playerInventory.player.level(), pos)
            val blockInv = SimpleContainer(BaseMinerBlockEntity.INVENTORY_SIZE)
            return MinerScreenHandler(syncId, playerInventory, blockInv, context, SimpleContainerData(propertyCount), isAdvanced = isAdvanced)
        }
    }
}

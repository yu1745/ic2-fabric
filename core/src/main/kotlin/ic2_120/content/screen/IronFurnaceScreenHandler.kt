package ic2_120.content.screen

import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.content.sync.IronFurnaceSync
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.FurnaceOutputSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
<<<<<<< HEAD
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = IronFurnaceBlock::class, inventorySize = IronFurnaceBlockEntity.INVENTORY_SIZE)
=======
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = IronFurnaceBlock::class)
>>>>>>> backup/1.21.1
class IronFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(IronFurnaceScreenHandler::class.type(), syncId) {

    val sync = IronFurnaceSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, 3)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, IronFurnaceBlockEntity.SLOT_INPUT, 55, 16, DEFAULT_SLOT_SPEC)
        addSlot(FurnaceOutputSlot(blockInventory, IronFurnaceBlockEntity.SLOT_OUTPUT, 116, 35, OUTPUT_SLOT_SPEC) {
            context.get({ world, pos ->
                val be = world.getBlockEntity(pos)
                if (be is IronFurnaceBlockEntity) be.dropStoredExperience()
            })
        })
        beSlotToHandlerIndex[IronFurnaceBlockEntity.SLOT_OUTPUT] = slots.size - 1
        addTrackedSlot(blockInventory, IronFurnaceBlockEntity.SLOT_FUEL, 55, 53, DEFAULT_SLOT_SPEC)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlot: Int, x: Int, y: Int, spec: SlotSpec) {
        val handlerIndex = slots.size
        addSlot(PredicateSlot(inventory, beSlot, x, y, spec))
        beSlotToHandlerIndex[beSlot] = handlerIndex
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id == BUTTON_ID_COLLECT_XP) {
            context.get({ world, pos ->
                val be = world.getBlockEntity(pos)
                if (be is IronFurnaceBlockEntity) be.collectXp(player)
            }, true)
            return true
        }
        return false
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
<<<<<<< HEAD
                index in PLAYER_INV_START until HOTBAR_END -> {
=======
                index in PLAYER_INV_START..HOTBAR_END -> {
>>>>>>> backup/1.21.1
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                    if (!moved) return ItemStack.EMPTY
                }
<<<<<<< HEAD
                else -> {
                    // 守卫：仅机器槽可落此（玩家槽已被区间检查全覆盖）。若未来条件收窄使玩家槽落入，
                    // 直接拒绝而非向含源槽的玩家区间 insertItem（同实例自合并会复制物品）。
                    if (index >= PLAYER_INV_START) return ItemStack.EMPTY
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
=======
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
>>>>>>> backup/1.21.1
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
            world.getBlockState(pos).block is IronFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 3
<<<<<<< HEAD
        const val HOTBAR_END = 39
=======
        const val HOTBAR_END = 38
>>>>>>> backup/1.21.1
        const val SLOT_SIZE = 18
        const val BUTTON_ID_COLLECT_XP = 0

        private val DEFAULT_SLOT_SPEC = SlotSpec()
        private val OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })

<<<<<<< HEAD
=======
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): IronFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(3)
            return IronFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
>>>>>>> backup/1.21.1
    }
}

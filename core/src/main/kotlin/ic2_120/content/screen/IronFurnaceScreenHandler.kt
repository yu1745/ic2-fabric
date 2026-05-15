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
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = IronFurnaceBlock::class)
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

    private fun addTrackedSlot(inventory: Inventory, beSlot: Int, spec: SlotSpec) {
        val handlerIndex = slots.size
        addSlot(PredicateSlot(inventory, beSlot, 0, 0, spec))
        beSlotToHandlerIndex[beSlot] = handlerIndex
    }

    init {
        checkSize(blockInventory, 3)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, IronFurnaceBlockEntity.SLOT_INPUT, DEFAULT_SLOT_SPEC)
        addSlot(FurnaceOutputSlot(blockInventory, IronFurnaceBlockEntity.SLOT_OUTPUT, 0, 0, OUTPUT_SLOT_SPEC) {
            context.get({ world, pos ->
                val be = world.getBlockEntity(pos)
                if (be is IronFurnaceBlockEntity) be.dropStoredExperience()
            })
        })
        // 输出槽也需要记录映射（用于 quickMove 判断）
        beSlotToHandlerIndex[IronFurnaceBlockEntity.SLOT_OUTPUT] = slots.size - 1
        addTrackedSlot(blockInventory, IronFurnaceBlockEntity.SLOT_FUEL, DEFAULT_SLOT_SPEC)

        // 玩家物品栏
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
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
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
            world.getBlockState(pos).block is IronFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 3
        const val HOTBAR_END = 38
        const val SLOT_SIZE = 18

        private val DEFAULT_SLOT_SPEC = SlotSpec()
        private val OUTPUT_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): IronFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(3)
            return IronFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

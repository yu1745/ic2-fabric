package ic2_120.content.screen

import ic2_120.content.block.ChunkLoaderBlock
import ic2_120.content.block.machines.ChunkLoaderBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.sync.ChunkLoaderSync
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

/**
 * 区块加载器 GUI 的 ScreenHandler。
 * 槽位：放电槽 + 玩家背包。
 */
@ModScreenHandler(block = ChunkLoaderBlock::class)
class ChunkLoaderScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ChunkLoaderScreenHandler::class.type(), syncId) {

    val sync = ChunkLoaderSync(SyncedDataView(propertyDelegate))

    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, ChunkLoaderBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        addSlot(PredicateSlot(blockInventory, ChunkLoaderBlockEntity.SLOT_DISCHARGING, 0, 0, dischargingSlotSpec))
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
            when {
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    if (stackInSlot.item is IBatteryItem) {
                        // 手动处理电池槽以避免insertItem修改堆叠引用导致计数错误
                        val dischargingSlot = slots[SLOT_DISCHARGING_INDEX]
                        if (!dischargingSlot.hasStack()) {
                            val singleBattery = stackInSlot.copy()
                            singleBattery.count = 1
                            dischargingSlot.stack = singleBattery
                            stackInSlot.decrement(1)
                            slot.markDirty()
                            dischargingSlot.markDirty()
                        }
                        // 如果放电槽已有电池，不做任何操作
                    } else {
                        if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                    }
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
            world.getBlockState(pos).block is ChunkLoaderBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_DISCHARGING_INDEX = 0
        const val PLAYER_INV_START = 1
        const val HOTBAR_END = 37

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ChunkLoaderScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val ctx = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(ChunkLoaderBlockEntity.INVENTORY_SIZE)
            return ChunkLoaderScreenHandler(syncId, playerInventory, blockInv, ctx, ArrayPropertyDelegate(propertyCount))
        }
    }
}

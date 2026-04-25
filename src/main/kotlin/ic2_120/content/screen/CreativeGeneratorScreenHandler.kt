package ic2_120.content.screen

import ic2_120.content.block.CreativeGeneratorBlock
import ic2_120.content.block.machines.CreativeGeneratorBlockEntity
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.sync.CreativeGeneratorSync
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
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = CreativeGeneratorBlock::class)
class CreativeGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(CreativeGeneratorScreenHandler::class.type(), syncId) {

    val sync = CreativeGeneratorSync(
        SyncedDataView(propertyDelegate),
        { net.minecraft.util.math.Direction.NORTH }
    )

    private val batterySlotSpec = SlotSpec(
        canInsert = { stack ->
            !stack.isEmpty && stack.canBeCharged()
        },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, CreativeGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 电池槽
        addSlot(PredicateSlot(blockInventory, CreativeGeneratorBlockEntity.BATTERY_SLOT, 0, 0, batterySlotSpec))

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
            when (index) {
                CreativeGeneratorBlockEntity.BATTERY_SLOT -> {
                    if (!insertItem(stackInSlot, 1, 37, true)) return ItemStack.EMPTY
                }
                in 1..36 -> {
                    if (batterySlotSpec.canInsert(stackInSlot)) {
                        if (!insertItem(stackInSlot, 0, 1, false)) return ItemStack.EMPTY
                    } else return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 1, 37, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is CreativeGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 1
        const val SLOT_SIZE = 18

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CreativeGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(CreativeGeneratorBlockEntity.INVENTORY_SIZE)
            return CreativeGeneratorScreenHandler(
                syncId,
                playerInventory,
                blockInv,
                context,
                ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}

package ic2_120.content.screen

import ic2_120.content.block.CreativeGeneratorBlock
import ic2_120.content.block.machines.CreativeGeneratorBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
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
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

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
            !stack.isEmpty && (stack.item is IBatteryItem || stack.item is IElectricTool)
        },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, CreativeGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 电池槽
        addSlot(PredicateSlot(
            blockInventory,
            CreativeGeneratorBlockEntity.BATTERY_SLOT,
            BATTERY_SLOT_X,
            BLOCK_SLOTS_Y,
            batterySlotSpec
        ))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
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
        const val BATTERY_SLOT_X = 80
        const val BLOCK_SLOTS_Y = 35
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_SIZE = 18

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

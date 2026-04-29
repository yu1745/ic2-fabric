package ic2_120_advanced_solar_addon.content.screen

import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlock
import ic2_120_advanced_solar_addon.content.block.MolecularTransformerBlockEntity
import ic2_120_advanced_solar_addon.content.sync.MolecularTransformerSync
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.Direction
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.type

@ModScreenHandler(block = MolecularTransformerBlock::class)
class MolecularTransformerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(MolecularTransformerScreenHandler::class.type(), syncId) {

    val sync = MolecularTransformerSync(
        schema = SyncedDataView(propertyDelegate),
        tier = MolecularTransformerBlockEntity.TIER,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        addProperties(propertyDelegate)

        // Machine slots
        addSlot(Slot(blockInventory, MolecularTransformerBlock.INPUT_SLOT, 0, 0))
        addSlot(Slot(blockInventory, MolecularTransformerBlock.OUTPUT_SLOT, 0, 0))

        // Player inventory
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
                index == MolecularTransformerBlock.INPUT_SLOT -> {
                    if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                }
                index == MolecularTransformerBlock.OUTPUT_SLOT -> {
                    if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                }
                index in 2..37 -> {
                    if (!insertItem(stackInSlot, MolecularTransformerBlock.INPUT_SLOT, 1, false)) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 2, 38, false)) return ItemStack.EMPTY
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
            player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        const val PLAYER_INV_START = 2

        @ScreenFactory
        @JvmStatic
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MolecularTransformerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInventory = net.minecraft.inventory.SimpleInventory(MolecularTransformerBlock.INVENTORY_SIZE)
            val propertyDelegate = net.minecraft.screen.ArrayPropertyDelegate(propertyCount)
            return MolecularTransformerScreenHandler(syncId, playerInventory, blockInventory, context, propertyDelegate)
        }
    }
}

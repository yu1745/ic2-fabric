package ic2_120.content.screen

import ic2_120.content.block.UvLampBlock
import ic2_120.content.block.machines.UvLampBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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

@ModScreenHandler(block = UvLampBlock::class)
class UvLampScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(UvLampScreenHandler::class.type(), syncId) {

    val sync = ic2_120.content.sync.UvLampSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, UvLampBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 升级槽（超频升级，最大堆叠 4）
        addSlot(PredicateSlot(blockInventory, UvLampBlockEntity.SLOT_UPGRADE, 0, 0, UPGRADE_SLOT_SPEC))

        // 玩家背包
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
        if (!slot.hasStack()) return stack

        val inSlot = slot.stack
        stack = inSlot.copy()

        when {
            index == SLOT_UPGRADE_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            index in PLAYER_INV_START..HOTBAR_END -> {
                val moved = SlotMoveHelper.insertIntoTargets(
                    inSlot,
                    listOf(SlotTarget(slots[SLOT_UPGRADE_INDEX], UPGRADE_SLOT_SPEC))
                )
                if (!moved) return ItemStack.EMPTY
            }
            else -> if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
        }

        if (inSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (inSlot.count == stack.count) return ItemStack.EMPTY
        slot.onTakeItem(player, inSlot)
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is UvLampBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_UPGRADE_INDEX = 0
        const val PLAYER_INV_START = 1
        const val HOTBAR_END = 36

        private val UPGRADE_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item is IUpgradeItem },
            maxItemCount = UvLampBlockEntity.MAX_OVERCLOCKERS
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): UvLampScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(UvLampBlockEntity.INVENTORY_SIZE)
            return UvLampScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

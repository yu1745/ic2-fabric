package ic2_120.content.screen

import ic2_120.content.block.RtGeneratorBlock
import ic2_120.content.block.machines.RtGeneratorBlockEntity
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.RtGeneratorSync
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

@ModScreenHandler(block = RtGeneratorBlock::class)
class RtGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(RtGeneratorScreenHandler::class.type(), syncId) {

    val sync = RtGeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        checkSize(blockInventory, 7)
        addProperties(propertyDelegate)

        repeat(6) { index ->
            addSlot(PredicateSlot(blockInventory, index, 0, 0, FUEL_SLOT_SPEC))
        }
        addSlot(PredicateSlot(blockInventory, RtGeneratorBlockEntity.BATTERY_SLOT, 0, 0, BATTERY_SLOT_SPEC))

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
                // 机器槽 -> 玩家物品栏
                index in 0..6 -> if (!insertItem(stackInSlot, 7, 43, true)) return ItemStack.EMPTY
                // 玩家物品栏 -> 机器槽
                index in 7..42 -> {
                    val fuelTargets = (0..5).map { SlotTarget(slots[it], FUEL_SLOT_SPEC) }
                    val batteryTarget = SlotTarget(slots[RtGeneratorBlockEntity.BATTERY_SLOT], BATTERY_SLOT_SPEC)
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        fuelTargets + batteryTarget
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 7, 43, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is RtGeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 7
        const val SLOT_SIZE = 18

        private val FUEL_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> RtGeneratorBlockEntity.isRtgPellet(stack) }
        )
        private val BATTERY_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.canBeCharged() }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): RtGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(7)
            return RtGeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

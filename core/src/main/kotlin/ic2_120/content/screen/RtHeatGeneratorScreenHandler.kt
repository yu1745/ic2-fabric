package ic2_120.content.screen

import ic2_120.content.block.RtHeatGeneratorBlock
import ic2_120.content.block.machines.RtHeatGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.sync.RtHeatGeneratorSync
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

@ModScreenHandler(block = RtHeatGeneratorBlock::class)
class RtHeatGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(RtHeatGeneratorScreenHandler::class.type(), syncId) {

    private val syncedView = SyncedDataView(propertyDelegate)
    private val heatFlow = HeatFlowSync(
        syncedView,
        object : HeatFlowSync.HeatProducer {
            override fun getLastGeneratedHeat(): Long = 0L
            override fun getLastOutputHeat(): Long = 0L
        }
    )
    val sync = RtHeatGeneratorSync(syncedView, heatFlow)

    init {
        checkSize(blockInventory, RtHeatGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        repeat(RtHeatGeneratorBlockEntity.INVENTORY_SIZE) { index ->
            addSlot(PredicateSlot(blockInventory, index, 0, 0, FUEL_SLOT_SPEC))
        }

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
                index in 0 until RtHeatGeneratorBlockEntity.INVENTORY_SIZE -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
                }
                // 玩家物品栏 -> 机器槽
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val targets = (0 until RtHeatGeneratorBlockEntity.INVENTORY_SIZE).map {
                        SlotTarget(slots[it], FUEL_SLOT_SPEC)
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(stackInSlot, targets)
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is RtHeatGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 6
        const val HOTBAR_END = 41

        private val FUEL_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> RtHeatGeneratorBlockEntity.isRtgPellet(stack) }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): RtHeatGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return RtHeatGeneratorScreenHandler(
                syncId,
                playerInventory,
                SimpleInventory(RtHeatGeneratorBlockEntity.INVENTORY_SIZE),
                context,
                ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}

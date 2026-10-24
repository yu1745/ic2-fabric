package ic2_120.content.screen

import ic2_120.compat.world

import ic2_120.compat.stack

import ic2_120.content.block.TradeOMatBlock
import ic2_120.content.block.machines.TradeOMatBlockEntityBase
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(names = ["trade_o_mat"])
class TradeOMatScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    val isOwner: Boolean
) : AbstractContainerMenu(TradeOMatScreenHandler::class.type(), syncId) {

    init {
        checkContainerSize(blockInventory, TradeOMatBlockEntityBase.INVENTORY_SIZE)

        if (isOwner) {
            // 所有者视图：demand/offer 左列，input/output 右列
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_DEMAND,
                OPEN_DEMAND_X, OPEN_DEMAND_Y, SlotSpec(canInsert = { true }, canTake = { true })))
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_OFFER,
                OPEN_OFFER_X, OPEN_OFFER_Y, SlotSpec(canInsert = { true }, canTake = { true })))
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_INPUT,
                OPEN_INPUT_X, OPEN_INPUT_Y, SlotSpec()))
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_OUTPUT,
                OPEN_OUTPUT_X, OPEN_OUTPUT_Y, SlotSpec(canInsert = { false }, canTake = { true })))
        } else {
            // 买家视图：demand/offer 左列只读，input/output 右列
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_DEMAND,
                CLOSED_DEMAND_X, CLOSED_DEMAND_Y, SlotSpec(canInsert = { false }, canTake = { false })))
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_OFFER,
                CLOSED_OFFER_X, CLOSED_OFFER_Y, SlotSpec(canInsert = { false }, canTake = { false })))
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_INPUT,
                CLOSED_INPUT_X, CLOSED_INPUT_Y, SlotSpec()))
            addSlot(PredicateSlot(blockInventory, TradeOMatBlockEntityBase.SLOT_OUTPUT,
                CLOSED_OUTPUT_X, CLOSED_OUTPUT_Y, SlotSpec(canInsert = { false }, canTake = { true })))
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            if (index in MACHINE_SLOT_START..MACHINE_SLOT_END) {
                if (!slot.canTakeItems(player)) return ItemStack.EMPTY
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                slot.onQuickTransfer(stackInSlot, stack)
            } else {
                if (!moveItemStackTo(stackInSlot, SLOT_INPUT_INDEX, SLOT_INPUT_INDEX + 1, false)) {
                    if (isOwner) {
                        if (!moveItemStackTo(stackInSlot, SLOT_DEMAND_INDEX, SLOT_DEMAND_INDEX + 1, false)) {
                            moveItemStackTo(stackInSlot, SLOT_OFFER_INDEX, SLOT_OFFER_INDEX + 1, false)
                        }
                    }
                    // 原兜底：向玩家背包区间 insertItem 已删除——目标区间含源槽自身，
                    // 会与自身合并导致数量翻倍（物品复制，同 ChunkLoader bug）。
                    // 机器槽全插不进时物品留在原位，下方 count 相等检查会返回 EMPTY。
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is TradeOMatBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        // Open（所有者）槽位坐标
        const val OPEN_DEMAND_X = 50
        const val OPEN_DEMAND_Y = 19
        const val OPEN_OFFER_X = 50
        const val OPEN_OFFER_Y = 53
        const val OPEN_INPUT_X = 80
        const val OPEN_INPUT_Y = 19
        const val OPEN_OUTPUT_X = 80
        const val OPEN_OUTPUT_Y = 53

        // Closed（买家）槽位坐标
        const val CLOSED_DEMAND_X = 50
        const val CLOSED_DEMAND_Y = 17
        const val CLOSED_OFFER_X = 50
        const val CLOSED_OFFER_Y = 38
        const val CLOSED_INPUT_X = 143
        const val CLOSED_INPUT_Y = 17
        const val CLOSED_OUTPUT_X = 143
        const val CLOSED_OUTPUT_Y = 53

        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142

        const val SLOT_DEMAND_INDEX = 0
        const val SLOT_OFFER_INDEX = 1
        const val SLOT_INPUT_INDEX = 2
        const val SLOT_OUTPUT_INDEX = 3
        const val MACHINE_SLOT_START = 0
        const val MACHINE_SLOT_END = 3
        const val PLAYER_INV_START = 4
        const val HOTBAR_END = 40

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): TradeOMatScreenHandler {
            val pos = buf.readBlockPos()
            val isOwner = buf.readBoolean()
            val context = ContainerLevelAccess.create(playerInventory.player.world, pos)
            val blockInv = SimpleContainer(TradeOMatBlockEntityBase.INVENTORY_SIZE)
            return TradeOMatScreenHandler(syncId, playerInventory, blockInv, context, isOwner)
        }
    }
}

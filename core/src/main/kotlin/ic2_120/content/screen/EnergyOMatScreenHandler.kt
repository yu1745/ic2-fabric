package ic2_120.content.screen

import ic2_120.compat.world

import ic2_120.compat.stack

import ic2_120.content.block.EnergyOMatBlock
import ic2_120.content.block.machines.EnergyOMatBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.block.machines.EnergyOMatSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.Direction

@ModScreenHandler(block = EnergyOMatBlock::class)
class EnergyOMatScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    propertyDelegate: ContainerData,
    val isOwner: Boolean
) : AbstractContainerMenu(EnergyOMatScreenHandler::class.type(), syncId) {

    val sync = EnergyOMatSync(
        SyncedDataView(propertyDelegate),
        getFacing = { context.evaluate({ w, p -> w.getBlockState(p).getValue(BlockStateProperties.HORIZONTAL_FACING) }, Direction.NORTH) }
    )

    init {
        checkContainerSize(blockInventory, EnergyOMatBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, EnergyOMatBlockEntity.SLOT_DEMAND,
            SLOT_DEMAND_X, SLOT_DEMAND_Y, SlotSpec(canInsert = { isOwner }, canTake = { isOwner })))
        addSlot(PredicateSlot(blockInventory, EnergyOMatBlockEntity.SLOT_INPUT,
            SLOT_INPUT_X, SLOT_INPUT_Y, SlotSpec()))
        addSlot(PredicateSlot(blockInventory, EnergyOMatBlockEntity.SLOT_CHARGE,
            SLOT_CHARGE_X, SLOT_CHARGE_Y, SlotSpec(maxItemCount = 1)))
        addSlot(PredicateSlot(blockInventory, EnergyOMatBlockEntity.SLOT_UPGRADE,
            SLOT_UPGRADE_X, SLOT_UPGRADE_Y, SlotSpec()))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (!isOwner) return true
        context.evaluate({ world, pos ->
            val be = world.getBlockEntity(pos) as? EnergyOMatBlockEntity ?: return@evaluate true
            val delta = when (id) {
                BUTTON_OFFER_DOWN_BIG -> -100000
                BUTTON_OFFER_DOWN_MID -> -10000
                BUTTON_OFFER_DOWN_SMALL -> -1000
                BUTTON_OFFER_UP_SMALL -> 1000
                BUTTON_OFFER_UP_MID -> 10000
                BUTTON_OFFER_UP_BIG -> 100000
                else -> return@evaluate true
            }
            be.adjustEuOffer(delta)
        }, true)
        return true
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            if (index in MACHINE_SLOT_START..MACHINE_SLOT_END) {
                if (!slot.mayPickup(player)) return ItemStack.EMPTY
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                slot.onQuickCraft(stackInSlot, stack)
            } else {
                if (!moveItemStackTo(stackInSlot, SLOT_INPUT_INDEX, SLOT_INPUT_INDEX + 1, false)) {
                    if (isOwner) {
                        if (!moveItemStackTo(stackInSlot, SLOT_DEMAND_INDEX, SLOT_DEMAND_INDEX + 1, false) &&
                            !moveItemStackTo(stackInSlot, SLOT_UPGRADE_INDEX, SLOT_UPGRADE_INDEX + 1, false)) {
                            moveItemStackTo(stackInSlot, SLOT_CHARGE_INDEX, SLOT_CHARGE_INDEX + 1, false)
                        }
                    } else {
                        moveItemStackTo(stackInSlot, SLOT_CHARGE_INDEX, SLOT_CHARGE_INDEX + 1, false)
                    }
                    // 原兜底：向玩家背包区间 insertItem 已删除——目标区间含源槽自身，
                    // 会与自身合并导致数量翻倍（物品复制，同 ChunkLoader bug）。
                    // 机器槽全插不进时物品留在原位，下方 count 相等检查会返回 EMPTY。
                }
            }
            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY)
            else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            world.getBlockState(pos).block is EnergyOMatBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_DEMAND_X = 24
        const val SLOT_DEMAND_Y = 17
        const val SLOT_INPUT_X = 60
        const val SLOT_INPUT_Y = 17
        const val SLOT_CHARGE_X = 60
        const val SLOT_CHARGE_Y = 53
        const val SLOT_UPGRADE_X = 24
        const val SLOT_UPGRADE_Y = 53

        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142

        const val SLOT_DEMAND_INDEX = 0
        const val SLOT_INPUT_INDEX = 1
        const val SLOT_CHARGE_INDEX = 2
        const val SLOT_UPGRADE_INDEX = 3
        const val MACHINE_SLOT_START = 0
        const val MACHINE_SLOT_END = 3
        const val PLAYER_INV_START = 4
        const val HOTBAR_END = 40

        const val BUTTON_OFFER_DOWN_BIG = 0
        const val BUTTON_OFFER_DOWN_MID = 1
        const val BUTTON_OFFER_DOWN_SMALL = 2
        const val BUTTON_OFFER_UP_SMALL = 3
        const val BUTTON_OFFER_UP_MID = 4
        const val BUTTON_OFFER_UP_BIG = 5

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): EnergyOMatScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val isOwner = buf.readBoolean()
            val context = ContainerLevelAccess.create(playerInventory.player.level(), pos)
            val blockInv = SimpleContainer(EnergyOMatBlockEntity.INVENTORY_SIZE)
            return EnergyOMatScreenHandler(syncId, playerInventory, blockInv, context, SimpleContainerData(propertyCount), isOwner)
        }
    }
}

package ic2_120.content.screen

import ic2_120.content.block.nuclear.ReactorFluidPortBlock
import ic2_120.content.block.nuclear.ReactorFluidPortBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
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
import net.minecraft.util.Identifier
import ic2_120.registry.annotation.ScreenFactory

/**
 * 反应堆流体接口的 ScreenHandler。
 * 提供一个升级槽用于安装流体管道升级。
 */
@ModScreenHandler(block = ReactorFluidPortBlock::class)
class ReactorFluidPortScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ReactorFluidPortScreenHandler::class.type(), syncId) {

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, ReactorFluidPortBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 升级槽（只有 1 个）
        addSlot(
            PredicateSlot(
                blockInventory,
                ReactorFluidPortBlockEntity.SLOT_UPGRADE_INDICES[0],
                0,
                0,
                upgradeSlotSpec
            )
        )

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }

        // 玩家快捷栏
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

        when (index) {
            UPGRADE_SLOT_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }

            in PLAYER_INV_START..HOTBAR_END -> {
                val upgradeTarget = SlotTarget(slots[UPGRADE_SLOT_INDEX], upgradeSlotSpec)
                val moved = SlotMoveHelper.insertIntoTargets(inSlot, listOf(upgradeTarget))
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
            world.getBlockState(pos).block is ReactorFluidPortBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val UPGRADE_SLOT_INDEX = 0
        const val PLAYER_INV_START = 1
        const val HOTBAR_END = 36

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ReactorFluidPortScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(ReactorFluidPortBlockEntity.INVENTORY_SIZE)
            return ReactorFluidPortScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

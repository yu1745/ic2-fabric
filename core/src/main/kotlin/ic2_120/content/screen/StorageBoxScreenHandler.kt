package ic2_120.content.screen

import ic2_120.content.block.storage.StorageBoxBlock
import ic2_120.content.block.storage.StorageBoxBlockEntity
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack

import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.network.PacketByteBuf
import ic2_120.registry.annotation.ScreenFactory

/**
 * 储物箱 GUI 的 ScreenHandler
 *
 * 所有容量的储物箱统一使用单列 9 格布局，容量通过 ScrollView 滚动显示。
 */
@ModScreenHandler(name = "storage_box")
class StorageBoxScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    val inventory: Inventory
) : ScreenHandler(StorageBoxScreenHandler::class.type(), syncId) {

    /** 玩家主背包首槽索引（随储物格数量变化） */
    val playerInventorySlotStart: Int get() = inventory.size()

    init {
        // 储物箱槽位
        val inventorySize = inventory.size()
        val columns = if (inventorySize == 126) 14 else 9
        val rows = (inventorySize + columns - 1) / columns
        val (slotStartX, slotStartY) = when (inventorySize) {
            27 -> 32 to 19
            45 -> 32 to 18
            63 -> 32 to 18
            126 -> 6 to 19
            else -> 32 to 19
        }

        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val slotIndex = row * columns + col
                if (slotIndex < inventorySize) {
                    addSlot(Slot(inventory, slotIndex, slotStartX + col * 18, slotStartY + row * 18))
                }
            }
        }

        // 玩家背包槽位（3x9 + 1x9 快捷栏）
        val (playerInvX, playerInvY) = when (inventorySize) {
            27 -> 32 to 79
            45 -> 32 to 119
            63 -> 32 to 153
            126 -> 51 to 194
            else -> 32 to 79
        }
        val hotbarY = playerInvY + 58
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, playerInvX + col * 18, playerInvY + row * 18))
            }
        }

        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, playerInvX + col * 18, hotbarY))
        }
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        var movedStack = ItemStack.EMPTY
        val slot = slots[slotIndex]
        if (slot.hasStack()) {
            val stack = slot.stack
            movedStack = stack.copy()

            // 从储物箱移到玩家背包
            if (slotIndex < inventory.size()) {
                if (!this.insertItem(stack, inventory.size(), slots.size, true)) {
                    return ItemStack.EMPTY
                }
            }
            // 从玩家背包移到储物箱：禁止放入可存储物品
            else {
                if (isStorageItem(stack)) return ItemStack.EMPTY
                if (!this.insertItem(stack, 0, inventory.size(), false)) {
                    return ItemStack.EMPTY
                }
            }

            if (stack.isEmpty) {
                slot.markDirty()
            } else {
                slot.markDirty()
            }
        }

        return movedStack
    }

    private fun isStorageItem(stack: ItemStack): Boolean {
        val item = stack.item
        if (item !is BlockItem) return false
        val block = item.block
        return block is ShulkerBoxBlock || block is StorageBoxBlock
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return inventory.canPlayerUse(player)
    }

    companion object {
        /**
         * 从 PacketByteBuf 创建 ScreenHandler（客户端）
         */
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): StorageBoxScreenHandler {
            val pos = buf.readBlockPos()
            val world = playerInventory.player.world
            val blockEntity = world.getBlockEntity(pos)

            if (blockEntity is StorageBoxBlockEntity) {
                return StorageBoxScreenHandler(syncId, playerInventory, blockEntity)
            }

            throw IllegalStateException(net.minecraft.text.Text.translatable("gui.ic2_120.storage_box.entity_not_found", pos).string)
        }

        /**
         * 直接从 BlockEntity 创建 ScreenHandler（服务端）
         */
        fun create(syncId: Int, playerInventory: PlayerInventory, blockEntity: StorageBoxBlockEntity): StorageBoxScreenHandler {
            return StorageBoxScreenHandler(syncId, playerInventory, blockEntity)
        }
    }
}

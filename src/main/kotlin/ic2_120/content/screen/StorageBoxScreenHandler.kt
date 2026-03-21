package ic2_120.content.screen

import ic2_120.content.block.storage.StorageBoxBlockEntity
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot

/**
 * 储物箱 GUI 的 ScreenHandler
 *
 * 包含储物箱的物品槽位和玩家背包槽位。
 * 所有材质的储物箱共享同一个 ScreenHandler。
 */
@ModScreenHandler(name = "storage_box")
class StorageBoxScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    val inventory: Inventory  // 改为 public 以便客户端访问
) : ScreenHandler(StorageBoxScreenHandler::class.type(), syncId) {

    /** 动态计算的玩家背包 Y 坐标 */
    val playerInventoryY: Int
    /** 动态计算的快捷栏 Y 坐标 */
    val hotbarY: Int

    init {
        // 储物箱槽位
        val inventorySize = inventory.size()

        // 计算布局：大于45格使用双列（向左扩展），否则单列
        // 木质(27)、铁质(45)、青铜(45) = 单列
        // 钢制(63)、铱(126) = 双列
        val useDoubleColumn = inventorySize > 45
        val rows = if (useDoubleColumn) {
            (inventorySize + 17) / 18  // 双列时每行18格
        } else {
            (inventorySize + 8) / 9   // 单列时每行9格
        }

        // 计算储物箱区域高度
        val boxSlotsHeight = if (useDoubleColumn) {
            val rightColumnSlots = inventorySize.coerceAtMost(81)
            val rightColumnRows = (rightColumnSlots + 8) / 9
            val rightColumnHeight = rightColumnRows * 18 + 18
            val leftColumnSlots = inventorySize - rightColumnSlots
            val leftColumnRows = (leftColumnSlots + 8) / 9
            val leftColumnHeight = leftColumnRows * 18 + 18
            maxOf(rightColumnHeight, leftColumnHeight)
        } else {
            rows * 18 + 18
        }

        // 计算玩家背包位置
        playerInventoryY = boxSlotsHeight + 14
        hotbarY = playerInventoryY + 58

        if (useDoubleColumn) {
            // 双列布局（钢制63格、铱126格）
            // 右列（主列）
            val rightColumnSlots = inventorySize.coerceAtMost(81)  // 右列最多81格
            val rightColumnRows = (rightColumnSlots + 8) / 9
            for (row in 0 until rightColumnRows) {
                for (col in 0 until 9) {
                    val slotIndex = row * 9 + col
                    if (slotIndex < inventorySize) {
                        addSlot(Slot(inventory, slotIndex, 8 + col * 18, 18 + row * 18))
                    }
                }
            }
            // 左列（向左扩展）
            val leftColumnStartIndex = rightColumnSlots
            val leftColumnSlots = inventorySize - rightColumnSlots
            val leftColumnRows = (leftColumnSlots + 8) / 9
            for (row in 0 until leftColumnRows) {
                for (col in 0 until 9) {
                    val slotIndex = leftColumnStartIndex + row * 9 + col
                    if (slotIndex < inventorySize) {
                        // 向左偏移：8 - 9 * 18 - 12 = -154
                        addSlot(Slot(inventory, slotIndex, -154 + col * 18, 18 + row * 18))
                    }
                }
            }
        } else {
            // 单列布局（木质27格、铁质45格、青铜45格）
            for (row in 0 until rows) {
                for (col in 0 until 9) {
                    val slotIndex = row * 9 + col
                    if (slotIndex < inventorySize) {
                        addSlot(Slot(inventory, slotIndex, 8 + col * 18, 18 + row * 18))
                    }
                }
            }
        }

        // 玩家背包槽位（3x9）
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18))
            }
        }

        // 玩家快捷栏槽位
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, hotbarY))
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
            // 从玩家背包移到储物箱
            else if (!this.insertItem(stack, 0, inventory.size(), false)) {
                return ItemStack.EMPTY
            }

            if (stack.isEmpty) {
                slot.markDirty()
            } else {
                slot.markDirty()
            }
        }

        return movedStack
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return inventory.canPlayerUse(player)
    }

    companion object {
        /**
         * 从 PacketByteBuf 创建 ScreenHandler（客户端）
         */
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): StorageBoxScreenHandler {
            val pos = buf.readBlockPos()
            val world = playerInventory.player.world
            val blockEntity = world.getBlockEntity(pos)

            if (blockEntity is StorageBoxBlockEntity) {
                return StorageBoxScreenHandler(syncId, playerInventory, blockEntity)
            }

            throw IllegalStateException("储物箱 BlockEntity 不存在于位置 $pos")
        }

        /**
         * 直接从 BlockEntity 创建 ScreenHandler（服务端）
         */
        fun create(syncId: Int, playerInventory: PlayerInventory, blockEntity: StorageBoxBlockEntity): StorageBoxScreenHandler {
            return StorageBoxScreenHandler(syncId, playerInventory, blockEntity)
        }
    }
}

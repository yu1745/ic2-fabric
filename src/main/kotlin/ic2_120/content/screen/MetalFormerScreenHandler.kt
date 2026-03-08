package ic2_120.content.screen

import ic2_120.content.sync.MetalFormerSync
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.block.machines.MetalFormerBlockEntity
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
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

@ModScreenHandler(block = MetalFormerBlock::class)
class MetalFormerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(MetalFormerScreenHandler::class), syncId) {

    val sync = MetalFormerSync(SyncedDataView(propertyDelegate))

    /**
     * 服务端处理：收到客户端发来的按钮点击包后在此执行。
     * 客户端应发送 [ButtonClickC2SPacket] 指定 syncId 与 [BUTTON_ID_MODE_CYCLE]。
     */
    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id != BUTTON_ID_MODE_CYCLE) return false
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is MetalFormerBlockEntity) {
                be.cycleMode()
            }
        }, true)
        return true
    }

    /**
     * 获取方块位置（用于客户端显示或其他需要）
     */
    fun getBlockPos(): net.minecraft.util.math.BlockPos? {
        return context.get({ world, pos -> pos }, null)
    }

    init {
        checkSize(blockInventory, MetalFormerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 机器槽位
        // 左上：输入槽
        addSlot(Slot(blockInventory, MetalFormerBlockEntity.SLOT_INPUT, INPUT_SLOT_X, INPUT_SLOT_Y))

        // 左下：放电槽（放置电池）
        addSlot(object : Slot(blockInventory, MetalFormerBlockEntity.SLOT_DISCHARGING, DISCHARGING_SLOT_X, DISCHARGING_SLOT_Y) {
            override fun canInsert(stack: ItemStack): Boolean {
                // TODO: 检查是否为电池物品
                return false
            }
        })

        // 中间右侧：输出槽
        addSlot(object : Slot(blockInventory, MetalFormerBlockEntity.SLOT_OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(player: PlayerEntity): Boolean = true
        })

        // 最右侧：升级槽
        addSlot(object : Slot(blockInventory, MetalFormerBlockEntity.SLOT_UPGRADE, UPGRADE_SLOT_X, UPGRADE_SLOT_Y) {
            override fun canInsert(stack: ItemStack): Boolean {
                // 仅在挤压模式下允许插入次要输入
                val mode = MetalFormerSync.Mode.fromId(propertyDelegate.get(2))
                return mode == MetalFormerSync.Mode.EXTRUDING
            }
        })

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }

        // 快捷栏
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
            when {
                // 输出槽 -> 玩家物品栏
                index == SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 放电槽 -> 玩家物品栏
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 升级槽 -> 玩家物品栏
                index == SLOT_UPGRADE_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 玩家物品栏 -> 机器
                index in PLAYER_INV_START..HOTBAR_END -> {
                    if (!insertItem(stackInSlot, SLOT_INPUT_INDEX, SLOT_INPUT_INDEX + 1, false)) {
                        return ItemStack.EMPTY
                    }
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is MetalFormerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        // 机器槽位位置
        const val INPUT_SLOT_X = 56
        const val INPUT_SLOT_Y = 35

        const val DISCHARGING_SLOT_X = 56
        const val DISCHARGING_SLOT_Y = 59

        const val OUTPUT_SLOT_X = 116
        const val OUTPUT_SLOT_Y = 47

        const val UPGRADE_SLOT_X = 152
        const val UPGRADE_SLOT_Y = 47

        const val SLOT_SIZE = 18

        // 玩家物品栏位置
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        // 槽位索引常量
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_DISCHARGING_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_UPGRADE_INDEX = 3
        const val PLAYER_INV_START = 4
        const val HOTBAR_END = 40

        /** 客户端发送按钮点击时使用此 id，服务端在 [onButtonClick] 中处理 */
        const val BUTTON_ID_MODE_CYCLE = 0

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MetalFormerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(MetalFormerBlockEntity.INVENTORY_SIZE)
            return MetalFormerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

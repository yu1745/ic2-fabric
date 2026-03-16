package ic2_120.content.screen

import ic2_120.content.block.NuclearReactorBlock
import ic2_120.content.block.machines.NuclearReactorBlockEntity
import ic2_120.content.reactor.IBaseReactorComponent
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
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

/**
 * 核反应堆 ScreenHandler。
 * 槽位数量根据打开时的容量动态确定（27–81），竖排：3 列→9 列，每列 9 行。
 * 若在打开界面时添加/移除反应仓，需关闭重开以刷新槽位数量。
 */
@ModScreenHandler(block = NuclearReactorBlock::class)
class NuclearReactorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    /** 打开时的反应堆槽位数量（27–81） */
    val reactorSlotCount: Int,
    /** 核反应堆方块实体 */
    val reactor: NuclearReactorBlockEntity? = null
) : ScreenHandler(NuclearReactorScreenHandler::class.type(), syncId) {

    val sync = NuclearReactorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        checkSize(blockInventory, reactorSlotCount)
        addProperties(propertyDelegate)

        // 槽位竖排：3 列→9 列，每列 9 行。index: col = i/9, row = i%9
        val reactorCols = (reactorSlotCount + 8) / 9
        for (index in 0 until reactorSlotCount) {
            val col = index / GRID_ROWS
            val row = index % GRID_ROWS
            val slotX = SLOT_GRID_X + col * SLOT_SIZE
            val slotY = SLOT_GRID_Y + row * SLOT_SIZE
            addSlot(PredicateSlot(blockInventory, index, slotX, slotY, REACTOR_SLOT_SPEC))
        }

        val gridHeight = GRID_ROWS * SLOT_SIZE
        val playerInvY = SLOT_GRID_Y + gridHeight + 10
        val hotbarY = playerInvY + 54

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, playerInvY + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, hotbarY))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index < reactorSlotCount -> {
                    if (!insertItem(stackInSlot, reactorSlotCount, slots.size, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                else -> {
                    val reactorTargets = (0 until reactorSlotCount).map {
                        SlotTarget(slots[it], REACTOR_SLOT_SPEC)
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(stackInSlot, reactorTargets)
                    if (!moved) {
                        return ItemStack.EMPTY
                    }
                }
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
            world.getBlockState(pos).block is NuclearReactorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    /** 反应堆槽位列数（3–9） */
    val reactorCols: Int get() = (reactorSlotCount + 8) / 9

    /** 玩家背包 Y 偏移（用于 Screen 绘制） */
    val playerInvY: Int get() = SLOT_GRID_Y + GRID_ROWS * SLOT_SIZE + 16

    /** 快捷栏 Y 偏移 */
    val hotbarY: Int get() = playerInvY + 58

    companion object {
        const val GRID_ROWS = 9
        /** 槽位区域居中：(FRAME_WIDTH - 能量条 - 间距 - 9*SLOT_SIZE - 间距 - 温度条) / 2 = 9 */
        const val SLOT_GRID_X = 31
        const val SLOT_GRID_Y = 18
        const val SLOT_SIZE = 18
        /** 固定界面框宽度（不随容量变化） */
        const val FRAME_WIDTH = 220
        /** 玩家背包 X 偏移（居中于 FRAME_WIDTH） */
        val PLAYER_INV_X = (FRAME_WIDTH - 9 * SLOT_SIZE) / 2 

        private val REACTOR_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> !stack.isEmpty && stack.item is IBaseReactorComponent }
        )

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): NuclearReactorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val capacity = buf.readVarInt().coerceIn(NuclearReactorSync.BASE_SLOTS, NuclearReactorBlockEntity.MAX_SLOTS)
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(capacity)
            // 获取核反应堆方块实体
            val reactor = playerInventory.player.world.getBlockEntity(pos) as? NuclearReactorBlockEntity
            return NuclearReactorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount), capacity, reactor)
        }
    }
}

package ic2_120.content.screen

import ic2_120.content.block.nuclear.NuclearReactorBlock
import ic2_120.content.block.nuclear.NuclearReactorBlockEntity
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
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
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import ic2_120.registry.annotation.ScreenFactory

/**
 * 核反应堆 ScreenHandler。
 * 槽位数量根据打开时的容量动态确定（27–81），竖排：3 列→9 列，每列 9 行。
 * 若在打开界面时添加/移除反应仓，需关闭重开以刷新槽位数量。
 *
 * 机器槽 GUI 坐标统一为 (0,0)，由客户端 [NuclearReactorScreen] Compose 锚点写回。
 *
 * 玩家栏纵坐标由格网与 [GRID_ROWS] 推导（[playerInvY] / [hotbarY]），不套用 [GuiSize] 固定分档。
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
    val reactor: NuclearReactorBlockEntity? = null,
    /** 是否为热模式（热模式时显示 4 个流体槽） */
    val isThermalMode: Boolean = false
) : ScreenHandler(NuclearReactorScreenHandler::class.type(), syncId) {

    val sync = NuclearReactorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    init {
        val invSize = if (isThermalMode) NuclearReactorBlockEntity.INVENTORY_SIZE else reactorSlotCount
        checkSize(blockInventory, invSize)
        addProperties(propertyDelegate)

        // 槽位竖排：3 列→9 列，每列 9 行。index: col = i/9, row = i%9（几何由客户端 Compose 布局）
        for (index in 0 until reactorSlotCount) {
            addSlot(PredicateSlot(blockInventory, index, 0, 0, REACTOR_SLOT_SPEC))
        }

        // 热模式：4 个流体槽（GUI 位置由客户端 Compose 布局）
        if (isThermalMode) {
            addSlot(PredicateSlot(blockInventory, NuclearReactorBlockEntity.SLOT_COOLANT_INPUT, 0, 0, COOLANT_INPUT_SPEC))
            addSlot(PredicateSlot(blockInventory, NuclearReactorBlockEntity.SLOT_COOLANT_OUTPUT, 0, 0, OUTPUT_ONLY_SPEC))
            addSlot(PredicateSlot(blockInventory, NuclearReactorBlockEntity.SLOT_HOT_COOLANT_INPUT, 0, 0, HOT_COOLANT_INPUT_SPEC))
            addSlot(PredicateSlot(blockInventory, NuclearReactorBlockEntity.SLOT_HOT_COOLANT_OUTPUT, 0, 0, OUTPUT_ONLY_SPEC))
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

    /** 流体槽起始索引（仅热模式时有） */
    private val fluidSlotStart: Int get() = reactorSlotCount
    private val fluidSlotEnd: Int get() = if (isThermalMode) reactorSlotCount + 4 else reactorSlotCount

    /** 玩家主背包首槽在 [slots] 中的索引（客户端 Compose 锚点与 quickMove 共用） */
    val playerInventorySlotStart: Int get() = fluidSlotEnd

    private val playerInvStart: Int get() = playerInventorySlotStart

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasStack()) return stack

        val stackInSlot = slot.stack
        stack = stackInSlot.copy()

        when {
            index < reactorSlotCount -> {
                if (!insertItem(stackInSlot, playerInvStart, slots.size, true)) return ItemStack.EMPTY
                slot.onQuickTransfer(stackInSlot, stack)
            }
            index in fluidSlotStart until fluidSlotEnd -> {
                if (!insertItem(stackInSlot, playerInvStart, slots.size, true)) return ItemStack.EMPTY
                slot.onQuickTransfer(stackInSlot, stack)
            }
            else -> {
                val reactorTargets = (0 until reactorSlotCount).map { SlotTarget(slots[it], REACTOR_SLOT_SPEC) }
                val fluidTargets = if (isThermalMode) listOf(
                    SlotTarget(slots[fluidSlotStart], COOLANT_INPUT_SPEC),
                    SlotTarget(slots[fluidSlotStart + 2], HOT_COOLANT_INPUT_SPEC)
                ) else emptyList()
                val moved = SlotMoveHelper.insertIntoTargets(stackInSlot, reactorTargets + fluidTargets)
                if (!moved) return ItemStack.EMPTY
            }
        }

        if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (stackInSlot.count == stack.count) return ItemStack.EMPTY
        slot.onTakeItem(player, stackInSlot)
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

        /** 冷却液输入：仅接受满冷却液容器（桶、冷却液单元、通用流体单元） */
        private val COOLANT_INPUT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item == ModFluids.COOLANT_BUCKET ||
                    Registries.ITEM.getId(stack.item) == Identifier("ic2_120", "coolant_cell") ||
                    (stack.item is FluidCellItem && stack.getFluidCellVariant()?.fluid?.let {
                        it == ModFluids.COOLANT_STILL || it == ModFluids.COOLANT_FLOWING
                    } == true)
            }
        )

        /** 热冷却液输入：仅接受空容器（用于提取热冷却液） */
        private val HOT_COOLANT_INPUT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item == Items.BUCKET ||
                    Registries.ITEM.getId(stack.item) == Identifier("ic2_120", "empty_cell") ||
                    (stack.item is FluidCellItem && stack.isFluidCellEmpty())
            }
        )

        /** 仅输出槽（空容器/满容器返回，不可放入） */
        private val OUTPUT_ONLY_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): NuclearReactorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val capacity = buf.readVarInt().coerceIn(NuclearReactorSync.BASE_SLOTS, NuclearReactorBlockEntity.MAX_SLOTS)
            val isThermal = buf.readBoolean()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(NuclearReactorBlockEntity.INVENTORY_SIZE)
            val reactor = playerInventory.player.world.getBlockEntity(pos) as? NuclearReactorBlockEntity
            return NuclearReactorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount), capacity, reactor, isThermal)
        }
    }
}

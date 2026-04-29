package ic2_120.content.screen

import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.block.machines.InductionFurnaceBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
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

/**
 * 感应炉 GUI 的 ScreenHandler。
 * 布局：左侧双输入槽 + 放电槽，右侧双输出槽。
 */
@ModScreenHandler(block = InductionFurnaceBlock::class)
class InductionFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(InductionFurnaceScreenHandler::class.type(), syncId) {

    val sync = InductionFurnaceSync(SyncedDataView(propertyDelegate))

    private val inputSlotSpec0 = SlotSpec(canInsert = { stack -> stack.item !is IBatteryItem })
    private val inputSlotSpec1 = SlotSpec(canInsert = { stack -> stack.item !is IBatteryItem })
    private val outputSlotSpec0 = SlotSpec(canInsert = { false }, canTake = { true })
    private val outputSlotSpec1 = SlotSpec(canInsert = { false }, canTake = { true })
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, InductionFurnaceBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_INPUT_0, 0, 0, inputSlotSpec0))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_INPUT_1, 0, 0, inputSlotSpec1))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_OUTPUT_0, 0, 0, outputSlotSpec0))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_OUTPUT_1, 0, 0, outputSlotSpec1))
        addSlot(PredicateSlot(blockInventory, InductionFurnaceBlockEntity.SLOT_DISCHARGING, 0, 0, dischargingSlotSpec))

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        // 快捷栏
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
                // 输出槽 -> 玩家物品栏
                index == SLOT_OUTPUT_0_INDEX || index == SLOT_OUTPUT_1_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 放电槽 -> 玩家物品栏
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 玩家物品栏/快捷栏 -> 机器
                index in PLAYER_INV_START until HOTBAR_END -> {
                    // 优先移动到输入槽
                    if (!insertItem(stackInSlot, SLOT_INPUT_0_INDEX, SLOT_INPUT_0_INDEX + 1, false)) {
                        if (!insertItem(stackInSlot, SLOT_INPUT_1_INDEX, SLOT_INPUT_1_INDEX + 1, false)) {
                            if (stackInSlot.item is IBatteryItem) {
                                // 电池只能放入放电槽，且只能放1个
                                // 手动处理以避免 insertItem 修改堆叠引用导致计数错误
                                val dischargingSlot = slots[SLOT_DISCHARGING_INDEX]
                                if (!dischargingSlot.hasStack()) {
                                    // 放电槽为空，放入1个电池
                                    val singleBattery = stackInSlot.copy()
                                    singleBattery.count = 1
                                    dischargingSlot.stack = singleBattery
                                    stackInSlot.decrement(1)
                                    slot.markDirty()
                                    dischargingSlot.markDirty()
                                }
                                // 如果放电槽已有电池，不做任何操作（电池保留在原位）
                            } else {
                                if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                            }
                        }
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
            world.getBlockState(pos).block is InductionFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        // 槽位索引
        const val SLOT_INPUT_0_INDEX = 0
        const val SLOT_INPUT_1_INDEX = 1
        const val SLOT_OUTPUT_0_INDEX = 2
        const val SLOT_OUTPUT_1_INDEX = 3
        const val SLOT_DISCHARGING_INDEX = 4
        const val PLAYER_INV_START = 5
        const val HOTBAR_END = 40

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): InductionFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(InductionFurnaceBlockEntity.INVENTORY_SIZE)
            return InductionFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

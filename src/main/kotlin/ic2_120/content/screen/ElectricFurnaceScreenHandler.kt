package ic2_120.content.screen

import ic2_120.content.sync.ElectricFurnaceSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.block.machines.ElectricFurnaceBlockEntity
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
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate

/**
 * 电炉 GUI 的 ScreenHandler。
 * 通过 SyncedDataView 按声明顺序自动对齐 index，无需手动指定。
 */
@ModScreenHandler(block = ElectricFurnaceBlock::class)
class ElectricFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ElectricFurnaceScreenHandler::class.type(), syncId) {

    val sync = ElectricFurnaceSync(SyncedDataView(propertyDelegate))

    private val inputSlotSpec = SlotSpec(canInsert = { stack -> stack.item !is IBatteryItem })
    private val outputSlotSpec = SlotSpec(canInsert = { false }, canTake = { true })
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, ElectricFurnaceBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        // 输入槽（左侧）、输出槽（右侧），同一行，留出上方给标题与能量条
        addSlot(PredicateSlot(blockInventory, ElectricFurnaceBlockEntity.SLOT_INPUT, INPUT_SLOT_X, BLOCK_SLOTS_Y, inputSlotSpec))
        addSlot(PredicateSlot(blockInventory, ElectricFurnaceBlockEntity.SLOT_OUTPUT, OUTPUT_SLOT_X, BLOCK_SLOTS_Y, outputSlotSpec))
        // 放电槽（第二行左侧）
        addSlot(PredicateSlot(blockInventory, ElectricFurnaceBlockEntity.SLOT_DISCHARGING, INPUT_SLOT_X, BLOCK_SLOTS_Y + SLOT_SIZE, dischargingSlotSpec))
        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    // 优先移动到输入槽，其次移动到放电槽
                    if (!insertItem(stackInSlot, SLOT_INPUT_INDEX, SLOT_INPUT_INDEX + 1, false)) {
                        if (stackInSlot.item is IBatteryItem && !insertItem(stackInSlot, SLOT_DISCHARGING_INDEX, SLOT_DISCHARGING_INDEX + 1, false)) {
                            if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                        }
                    }
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty()) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is ElectricFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        /** 输入槽 X（与 MC 熔炉一致） */
        const val INPUT_SLOT_X = 56
        /** 输出槽 X */
        const val OUTPUT_SLOT_X = 116
        /** 机器槽（输入/输出）Y，同一行，置于标题与能量条下方避免重叠 */
        const val BLOCK_SLOTS_Y = 54
        /** 玩家背包 3 行起始 Y */
        const val PLAYER_INV_Y = 84
        /** 快捷栏 Y */
        const val HOTBAR_Y = 142
        /** 槽尺寸（用于客户端绘制边框等） */
        const val SLOT_SIZE = 18

        const val SLOT_INPUT_INDEX = 0
        const val SLOT_OUTPUT_INDEX = 1
        const val SLOT_DISCHARGING_INDEX = 2
        const val PLAYER_INV_START = 3
        const val HOTBAR_END = 39

        /** 客户端从 ExtendedScreenHandlerType 创建：从 buf 读取 pos，用临时 Inventory。 */
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ElectricFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(ElectricFurnaceBlockEntity.INVENTORY_SIZE)
            return ElectricFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

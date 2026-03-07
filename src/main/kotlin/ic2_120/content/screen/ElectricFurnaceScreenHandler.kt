package ic2_120.content.screen

import ic2_120.content.ElectricFurnaceSync
import ic2_120.content.SyncedDataView
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.registry.annotation.ModScreenHandler
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
) : ScreenHandler(ModScreenHandlers.getType(ElectricFurnaceScreenHandler::class), syncId) {

    val sync = ElectricFurnaceSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, 2)
        addProperties(propertyDelegate)
        // 输入槽
        addSlot(Slot(blockInventory, 0, 56, 17))
        // 输出槽
        addSlot(Slot(blockInventory, 1, 116, 35))
        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == 1 -> {
                    if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in 2..37 -> {
                    if (!insertItem(stackInSlot, 0, 1, false)) return ItemStack.EMPTY
                    if (!insertItem(stackInSlot, 1, 2, false)) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 2, 38, false)) return ItemStack.EMPTY
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
        /** 客户端从 ExtendedScreenHandlerType 创建：从 buf 读取 pos，用临时 Inventory。 */
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ElectricFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(2)
            return ElectricFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

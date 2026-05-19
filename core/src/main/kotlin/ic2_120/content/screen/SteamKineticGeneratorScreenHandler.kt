package ic2_120.content.screen

import ic2_120.content.block.SteamKineticGeneratorBlock
import ic2_120.content.block.machines.SteamKineticGeneratorBlockEntity
import ic2_120.content.item.SteamTurbine
import ic2_120.content.sync.SteamKineticGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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
 * 蒸汽动能发电机 ScreenHandler — 对齐 ic2_origin ContainerSteamKineticGenerator。
 * 1 个涡轮槽 + 玩家背包。
 */
@ModScreenHandler(block = SteamKineticGeneratorBlock::class)
class SteamKineticGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(SteamKineticGeneratorScreenHandler::class.type(), syncId) {

    val sync = SteamKineticGeneratorSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, SteamKineticGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        // 涡轮槽 (SlotGrid at 80,26 in ic2_origin)
        addSlot(Slot(blockInventory, SteamKineticGeneratorBlockEntity.SLOT_TURBINE, TURBINE_SLOT_X, TURBINE_SLOT_Y))
        // 玩家背包
        for (row in 0 until 3)
            for (col in 0 until 9)
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
        for (col in 0 until 9)
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val s = slot.stack; stack = s.copy()
            if (index == SLOT_TURBINE_INDEX) {
                if (!insertItem(s, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY
            } else if (s.item is SteamTurbine) {
                if (!insertItem(s, SLOT_TURBINE_INDEX, SLOT_TURBINE_INDEX + 1, false)) return ItemStack.EMPTY
            } else return ItemStack.EMPTY
            if (s.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (s.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, s)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is SteamKineticGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val TURBINE_SLOT_X = 80
        const val TURBINE_SLOT_Y = 26
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_TURBINE_INDEX = 0
        const val PLAYER_INV_START = 1
        const val PLAYER_INV_END = 37

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SteamKineticGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(SteamKineticGeneratorBlockEntity.INVENTORY_SIZE)
            return SteamKineticGeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

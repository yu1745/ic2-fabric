package ic2_120.content.screen

import ic2_120.content.block.SteamKineticGeneratorBlock
import ic2_120.content.block.machines.SteamKineticGeneratorBlockEntity
import ic2_120.content.item.SteamTurbine
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.SteamKineticGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

/**
 * 蒸汽动能发电机 ScreenHandler。
 * 1 个涡轮槽 + 1 个升级槽 + 玩家背包。
 */
@ModScreenHandler(block = SteamKineticGeneratorBlock::class, inventorySize = SteamKineticGeneratorBlockEntity.INVENTORY_SIZE)
class SteamKineticGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(SteamKineticGeneratorScreenHandler::class.type(), syncId) {

    val sync = SteamKineticGeneratorSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, SteamKineticGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        // 涡轮槽
        addSlot(Slot(blockInventory, SteamKineticGeneratorBlockEntity.SLOT_TURBINE, TURBINE_SLOT_X, TURBINE_SLOT_Y))
        beSlotToHandlerIndex[SteamKineticGeneratorBlockEntity.SLOT_TURBINE] = SLOT_TURBINE_INDEX
        // 升级槽
        val upgradeSpec = itemStorage?.deriveSlotSpec(SteamKineticGeneratorBlockEntity.SLOT_UPGRADE) ?: SlotSpec()
        addSlot(PredicateSlot(blockInventory, SteamKineticGeneratorBlockEntity.SLOT_UPGRADE, UPGRADE_SLOT_X, UPGRADE_SLOT_Y, upgradeSpec))
        beSlotToHandlerIndex[SteamKineticGeneratorBlockEntity.SLOT_UPGRADE] = SLOT_UPGRADE_INDEX
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
            when (index) {
                SLOT_TURBINE_INDEX -> {
                    if (!insertItem(s, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY
                }
                SLOT_UPGRADE_INDEX -> {
                    if (!insertItem(s, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY
                }
                in PLAYER_INV_START..PLAYER_INV_END -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    if (s.item is SteamTurbine) {
                        if (!insertItem(s, SLOT_TURBINE_INDEX, SLOT_TURBINE_INDEX + 1, false)) return ItemStack.EMPTY
                    } else {
                        val moved = SlotMoveHelper.insertFromRoutes(s, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                        if (!moved) return ItemStack.EMPTY
                    }
                }
                else -> {
                    if (!insertItem(s, PLAYER_INV_START, PLAYER_INV_END, false)) return ItemStack.EMPTY
                }
            }
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
        const val UPGRADE_SLOT_X = 152
        const val UPGRADE_SLOT_Y = 26
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 84
        const val HOTBAR_Y = 142
        const val SLOT_TURBINE_INDEX = 0
        const val SLOT_UPGRADE_INDEX = 1
        const val PLAYER_INV_START = 2
        const val PLAYER_INV_END = 38

    }
}

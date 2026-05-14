package ic2_120.content.screen

import ic2_120.content.sync.MaceratorSync
import ic2_120.content.block.MaceratorBlock
import ic2_120.content.block.machines.MaceratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
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
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = MaceratorBlock::class)
class MaceratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(MaceratorScreenHandler::class.type(), syncId) {

    val sync = MaceratorSync(SyncedDataView(propertyDelegate))

    /**
     * BlockEntity slot index → ScreenHandler slot index 的映射。
     * 在 init 块中 addSlot 时构建。
     */
    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkSize(blockInventory, MaceratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 机器槽位：从 RoutedItemStorage 派生 SlotSpec（单一数据源）
        addTrackedSlot(blockInventory, MaceratorBlockEntity.SLOT_INPUT)
        addTrackedSlot(blockInventory, MaceratorBlockEntity.SLOT_DISCHARGING)
        addTrackedSlot(blockInventory, MaceratorBlockEntity.SLOT_OUTPUT)
        for (i in MaceratorBlockEntity.SLOT_UPGRADE_INDICES.indices) {
            addTrackedSlot(blockInventory, MaceratorBlockEntity.SLOT_UPGRADE_INDICES[i])
        }

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    /**
     * 添加一个 PredicateSlot 并同时记录 BE slot index → handler slot index 映射。
     * SlotSpec 从 [itemStorage] 派生（单一数据源）；客户端无 itemStorage 时使用 [fallbackSpecs]。
     */
    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, fallbackSpec: SlotSpec? = null) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: fallbackSpec ?: DEFAULT_SLOT_SPEC
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, 0, 0, spec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()

            // PredicateSlot 的 index 即 BE 的 slot index；玩家槽不是 PredicateSlot，返回 -1
            val beSlot = (slot as? PredicateSlot)?.index ?: -1

            when {
                // 机器内部槽位（输出/放电/升级/输入）→ 玩家物品栏
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 玩家物品栏 → 机器（通过 RoutedItemStorage 路由）
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY // 客户端不应走到这里
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot,
                        storage,
                        storage.insertRoutes,
                        beSlotToHandlerIndex,
                        slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                // 快捷栏 ↔ 主背包互换
                else -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is MaceratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        private val DEFAULT_SLOT_SPEC = SlotSpec() // 客户端 fallback（宽松默认）

        // 槽位索引常量（客户端 Screen 的 SlotHost 需要）
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_DISCHARGING_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MaceratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(MaceratorBlockEntity.INVENTORY_SIZE)
            return MaceratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

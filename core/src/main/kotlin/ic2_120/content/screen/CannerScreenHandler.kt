package ic2_120.content.screen

import ic2_120.content.block.CannerBlock
import ic2_120.content.block.machines.CannerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.CannerSync
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
import net.minecraft.text.Text
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = CannerBlock::class)
class CannerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(CannerScreenHandler::class.type(), syncId) {

    val sync = CannerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(slot: Slot, beSlotIndex: Int) {
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    init {
        checkSize(blockInventory, CannerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 左上容器输入 (41,16)
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_CONTAINER, 41, 16,
            itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_CONTAINER) ?: SlotSpec()
        ), CannerBlockEntity.SLOT_CONTAINER)

        // 中列材料输入 (80,43) — 根据模式锁定
        val materialSpec = itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_MATERIAL) ?: SlotSpec()
        addTrackedSlot(object : PredicateSlot(blockInventory, CannerBlockEntity.SLOT_MATERIAL, 80, 43, materialSpec) {
            override fun canInsert(stack: ItemStack): Boolean {
                val mode = sync.getMode()
                if (mode == CannerSync.Mode.EMPTY_LIQUID || mode == CannerSync.Mode.BOTTLE_LIQUID) return false
                return super.canInsert(stack)
            }
        }, CannerBlockEntity.SLOT_MATERIAL)

        // 右侧输出 (119,16)
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_OUTPUT, 119, 16,
            itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_OUTPUT) ?: SlotSpec(canInsert = { false }, canTake = { true })
        ), CannerBlockEntity.SLOT_OUTPUT)

        // 放电槽 (8,79)
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_DISCHARGING, 8, 79,
            itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_DISCHARGING) ?: SlotSpec()
        ), CannerBlockEntity.SLOT_DISCHARGING)

        // 升级槽 (152,25) (152,43) (152,61) (152,79)
        val upgradeYs = intArrayOf(25, 43, 61, 79)
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                PredicateSlot(blockInventory, CannerBlockEntity.SLOT_UPGRADE_INDICES[i], 152, upgradeYs[i], upgradeSlotSpec),
                CannerBlockEntity.SLOT_UPGRADE_INDICES[i]
            )
        }

        // 左液槽底部空容器输出 (0,0) — ComposeUI 已不再使用，放无效位置
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_LEFT_EMPTY, -1000, -1000,
            itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_LEFT_EMPTY) ?: SlotSpec(canInsert = { false }, canTake = { true })
        ), CannerBlockEntity.SLOT_LEFT_EMPTY)

        // 右液槽顶部空容器输入 (0,0) — ComposeUI 已不再使用，放无效位置
        addTrackedSlot(PredicateSlot(blockInventory, CannerBlockEntity.SLOT_RIGHT_INPUT, -1000, -1000,
            itemStorage?.deriveSlotSpec(CannerBlockEntity.SLOT_RIGHT_INPUT) ?: SlotSpec()
        ), CannerBlockEntity.SLOT_RIGHT_INPUT)

        // 玩家背包 (8,102) 3行 + 快捷栏 (8,160)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 101 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 159))
        }
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is CannerBlockEntity) {
                when (id) {
                    BUTTON_ID_MODE_CYCLE -> be.cycleMode()
                    BUTTON_ID_SWAP_TANKS -> {
                        val leftBefore = be.getLeftFluidAmount()
                        val rightBefore = be.getRightFluidAmount()
                        val changed = be.swapTanks()
                        val leftAfter = be.getLeftFluidAmount()
                        val rightAfter = be.getRightFluidAmount()
                        player.sendMessage(
                            Text.translatable(
                                if (changed) "gui.ic2_120.canner.tanks_swapped"
                                else "gui.ic2_120.canner.tanks_swap_unchanged",
                                *if (changed) arrayOf(leftBefore, leftAfter, rightBefore, rightAfter)
                                else arrayOf(leftAfter, rightAfter)
                            ), true
                        )
                    }
                    else -> return@get
                }
            }
        }, true)
        return id == BUTTON_ID_MODE_CYCLE || id == BUTTON_ID_SWAP_TANKS
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_OUTPUT_INDEX || index == SLOT_LEFT_EMPTY_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                    if (!moved) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is CannerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_CONTAINER_INDEX = 0
        const val SLOT_MATERIAL_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val SLOT_LEFT_EMPTY_INDEX = 8
        const val SLOT_RIGHT_INPUT_INDEX = 9
        const val PLAYER_INV_START = 10
        const val HOTBAR_END = 46
        const val BUTTON_ID_MODE_CYCLE = 0
        const val BUTTON_ID_SWAP_TANKS = 1

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): CannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(CannerBlockEntity.INVENTORY_SIZE)
            return CannerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

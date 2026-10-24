package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.content.block.MatterGeneratorBlock
import ic2_120.content.block.machines.MatterGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.MatterGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(block = MatterGeneratorBlock::class, inventorySize = MatterGeneratorBlockEntity.INVENTORY_SIZE)
class MatterGeneratorScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(MatterGeneratorScreenHandler::class.type(), syncId) {

    val sync = MatterGeneratorSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkContainerSize(blockInventory, MatterGeneratorBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_SCRAP, 72, 40)
        addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_CONTAINER_INPUT, 125, 23)
        addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_CONTAINER_OUTPUT, 125, 59)

        val upgradeYs = intArrayOf(8, 26, 44, 62)
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(blockInventory, MatterGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i], 152, upgradeYs[i], upgradeSlotSpec)
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    private fun addTrackedSlot(inventory: Container, beSlotIndex: Int, x: Int, y: Int, fallbackSpec: SlotSpec? = null) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: fallbackSpec ?: DEFAULT_SLOT_SPEC
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                    if (!moved) return ItemStack.EMPTY
                }
                else -> {
                    // 守卫：仅机器槽可落此（玩家槽已被区间检查全覆盖）。若未来条件收窄使玩家槽落入，
                    // 直接拒绝而非向含源槽的玩家区间 insertItem（同实例自合并会复制物品）。
                    if (index >= PLAYER_INV_START) return ItemStack.EMPTY
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is MatterGeneratorBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        private val DEFAULT_SLOT_SPEC = SlotSpec()

        const val SLOT_SCRAP_INDEX = 0
        const val SLOT_CONTAINER_INPUT_INDEX = 1
        const val SLOT_CONTAINER_OUTPUT_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

    }
}

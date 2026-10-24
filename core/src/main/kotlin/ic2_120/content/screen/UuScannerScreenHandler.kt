package ic2_120.content.screen

import ic2_120.compat.world

import ic2_120.compat.stack

import ic2_120.content.block.UuScannerBlock
import ic2_120.content.block.machines.UuScannerBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.UuScannerSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(block = UuScannerBlock::class)
class UuScannerScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    val blockPos: net.minecraft.core.BlockPos,
    private val context: ContainerLevelAccess,
    propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(UuScannerScreenHandler::class.type(), syncId) {

    val sync = UuScannerSync(
        SyncedDataView(propertyDelegate),
        { null },
        { UuScannerSync.ENERGY_CAPACITY }
    )

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkContainerSize(blockInventory, UuScannerBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        // 输入槽 (54,34)
        addTrackedSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_INPUT, 54, 34,
            deriveSpec(UuScannerBlockEntity.SLOT_INPUT)))
        // 电池槽 (7,42)
        addTrackedSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_DISCHARGING, 7, 42,
            deriveSpec(UuScannerBlockEntity.SLOT_DISCHARGING)))
        // 水晶槽 (151,64)
        addTrackedSlot(PredicateSlot(blockInventory, UuScannerBlockEntity.SLOT_CRYSTAL, 151, 64,
            deriveSpec(UuScannerBlockEntity.SLOT_CRYSTAL)))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 7 + col * 18, 83 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 7 + col * 18, 141))
        }
    }

    private fun addTrackedSlot(slot: PredicateSlot) {
        beSlotToHandlerIndex[slot.index] = slots.size
        addSlot(slot)
    }

    private fun deriveSpec(beSlot: Int) = itemStorage?.deriveSlotSpec(beSlot) ?: SlotSpec()

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        context.evaluate({ world, pos ->
            val be = world.getBlockEntity(pos) as? UuScannerBlockEntity ?: return@evaluate false
            when (id) {
                BUTTON_DELETE_TEMPLATE -> be.deleteTemplate()
                BUTTON_SAVE_TO_CRYSTAL -> be.saveTemplateToCrystal()
            }
            true
        }, false)
        return id == BUTTON_DELETE_TEMPLATE || id == BUTTON_SAVE_TO_CRYSTAL
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index in SLOT_INPUT_INDEX..SLOT_CRYSTAL_INDEX -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
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
            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            world.getBlockState(pos).block is UuScannerBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_BATTERY_INDEX = 1
        const val SLOT_CRYSTAL_INDEX = 2
        const val PLAYER_INV_START = 3
        const val HOTBAR_END = 39

        const val BUTTON_DELETE_TEMPLATE = 1
        const val BUTTON_SAVE_TO_CRYSTAL = 2

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): UuScannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            return UuScannerScreenHandler(
                syncId, playerInventory,
                SimpleContainer(UuScannerBlockEntity.INVENTORY_SIZE),
                pos,
                ContainerLevelAccess.create(playerInventory.player.level(), pos),
                SimpleContainerData(propertyCount)
            )
        }
    }
}

package ic2_120.content.screen

import ic2_120.compat.world

import ic2_120.compat.stack

import ic2_120.content.block.PatternStorageBlock
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.core.BlockPos

@ModScreenHandler(block = PatternStorageBlock::class)
class PatternStorageScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    val blockPos: BlockPos,
    private val context: ContainerLevelAccess,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(PatternStorageScreenHandler::class.type(), syncId) {

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    init {
        checkContainerSize(blockInventory, PatternStorageBlockEntity.INVENTORY_SIZE)

        // 水晶槽 (152,29)
        addTrackedSlot(PredicateSlot(blockInventory, PatternStorageBlockEntity.SLOT_CRYSTAL, 152, 29, deriveSpec(PatternStorageBlockEntity.SLOT_CRYSTAL)))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    private fun addTrackedSlot(slot: PredicateSlot): PredicateSlot {
        beSlotToHandlerIndex[slot.index] = slots.size
        addSlot(slot)
        return slot
    }

    private fun deriveSpec(beSlot: Int) =
        itemStorage?.deriveSlotSpec(beSlot) ?: SlotSpec()

    fun getPatternStorage(world: net.minecraft.world.level.Level): PatternStorageBlockEntity? =
        world.getBlockEntity(blockPos) as? PatternStorageBlockEntity

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos) as? PatternStorageBlockEntity ?: return@get false
            when {
                id == BUTTON_EXPORT_TO_CRYSTAL -> be.exportSelectedTemplateToCrystal()
                id == BUTTON_IMPORT_FROM_CRYSTAL -> be.importTemplateFromCrystal()
                id >= BUTTON_SELECT_BASE -> be.selectTemplate(id - BUTTON_SELECT_BASE)
                else -> false
            }
        }, false)
        return true
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                SLOT_CRYSTAL_INDEX -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                }
                in PLAYER_INV_START until HOTBAR_END -> {
                    val storage = itemStorage ?: return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot,
                        storage,
                        storage.insertRoutes,
                        beSlotToHandlerIndex,
                        slots
                    )
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
            world.getBlockState(pos).block is PatternStorageBlock &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_CRYSTAL_INDEX = 0
        const val PLAYER_INV_START = 1
        const val HOTBAR_END = 37

        const val BUTTON_EXPORT_TO_CRYSTAL = 1
        const val BUTTON_IMPORT_FROM_CRYSTAL = 2
        const val BUTTON_SELECT_BASE = 1000

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): PatternStorageScreenHandler {
            val pos = buf.readBlockPos()
            buf.readVarInt()
            return PatternStorageScreenHandler(
                syncId,
                playerInventory,
                SimpleContainer(PatternStorageBlockEntity.INVENTORY_SIZE),
                pos,
                ContainerLevelAccess.create(playerInventory.player.world, pos)
            )
        }
    }
}

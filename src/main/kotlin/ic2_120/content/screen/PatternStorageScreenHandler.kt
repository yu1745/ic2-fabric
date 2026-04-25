package ic2_120.content.screen

import ic2_120.Ic2_120
import ic2_120.content.block.PatternStorageBlock
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

@ModScreenHandler(block = PatternStorageBlock::class)
class PatternStorageScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    val blockPos: BlockPos,
    private val context: ScreenHandlerContext
) : ScreenHandler(PatternStorageScreenHandler::class.type(), syncId) {

    init {
        checkSize(blockInventory, PatternStorageBlockEntity.INVENTORY_SIZE)

        // All slot coordinates are anchored by the client screen at render time.
        addSlot(PredicateSlot(blockInventory, PatternStorageBlockEntity.SLOT_CRYSTAL, 0, 0, CRYSTAL_SLOT_SPEC))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    fun getPatternStorage(world: net.minecraft.world.World): PatternStorageBlockEntity? =
        world.getBlockEntity(blockPos) as? PatternStorageBlockEntity

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
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

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                SLOT_CRYSTAL_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                }
                in PLAYER_INV_START..HOTBAR_END -> {
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(SlotTarget(slots[SLOT_CRYSTAL_INDEX], CRYSTAL_SLOT_SPEC))
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is PatternStorageBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        private val CRYSTAL_MEMORY_ID = Identifier.of(Ic2_120.MOD_ID, "crystal_memory")

        private val CRYSTAL_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> !stack.isEmpty && Registries.ITEM.getId(stack.item) == CRYSTAL_MEMORY_ID }
        )

        const val SLOT_CRYSTAL_INDEX = 0
        const val PLAYER_INV_START = 1
        const val HOTBAR_END = 37

        const val BUTTON_EXPORT_TO_CRYSTAL = 1
        const val BUTTON_IMPORT_FROM_CRYSTAL = 2
        const val BUTTON_SELECT_BASE = 1000

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): PatternStorageScreenHandler {
            val pos = buf.readBlockPos()
            buf.readVarInt()
            return PatternStorageScreenHandler(
                syncId,
                playerInventory,
                SimpleInventory(PatternStorageBlockEntity.INVENTORY_SIZE),
                pos,
                ScreenHandlerContext.create(playerInventory.player.world, pos)
            )
        }
    }
}

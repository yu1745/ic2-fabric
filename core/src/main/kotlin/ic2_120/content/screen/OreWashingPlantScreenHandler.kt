package ic2_120.content.screen

import ic2_120.content.block.OreWashingPlantBlock
import ic2_120.content.block.machines.OreWashingPlantBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.OreWashingPlantSync
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

@ModScreenHandler(block = OreWashingPlantBlock::class)
class OreWashingPlantScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(OreWashingPlantScreenHandler::class.type(), syncId) {

    val sync = OreWashingPlantSync(SyncedDataView(propertyDelegate))

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int, fallbackSpec: SlotSpec? = null) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: fallbackSpec ?: DEFAULT_SLOT_SPEC
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    init {
        checkSize(blockInventory, OreWashingPlantBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_INPUT_ORE, 104, 16)
        addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_INPUT_WATER, 38, 16)
        addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_1, 86, 61)
        addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_2, 104, 61)
        addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_3, 122, 61)
        addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_EMPTY, 38, 61)
        addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_DISCHARGING, 8, 61)

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_UPGRADE_INDICES[i], 152, 7 + i * 18, upgradeSlotSpec)
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

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val beSlot = (slot as? PredicateSlot)?.index ?: -1
            when {
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val storage = itemStorage
                    if (storage == null) return ItemStack.EMPTY
                    val moved = SlotMoveHelper.insertFromRoutes(
                        stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
                    )
                    if (!moved) return ItemStack.EMPTY
                }
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
            world.getBlockState(pos).block is OreWashingPlantBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18
        private val DEFAULT_SLOT_SPEC = SlotSpec()

        const val SLOT_INPUT_ORE_INDEX = 0
        const val SLOT_INPUT_WATER_INDEX = 1
        const val SLOT_OUTPUT_1_INDEX = 2
        const val SLOT_OUTPUT_2_INDEX = 3
        const val SLOT_OUTPUT_3_INDEX = 4
        const val SLOT_OUTPUT_EMPTY_INDEX = 5
        const val SLOT_DISCHARGING_INDEX = 6
        const val SLOT_UPGRADE_INDEX_START = 7
        const val SLOT_UPGRADE_INDEX_END = 10
        const val PLAYER_INV_START = 11
        const val HOTBAR_END = 47

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): OreWashingPlantScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(OreWashingPlantBlockEntity.INVENTORY_SIZE)
            return OreWashingPlantScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

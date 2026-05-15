package ic2_120.content.screen

import ic2_120.content.block.FermenterBlock
import ic2_120.content.block.machines.FermenterBlockEntity
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.FermenterSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = FermenterBlock::class)
class FermenterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(FermenterScreenHandler::class.type(), syncId) {

    val sync = FermenterSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    /** BE 槽位索引 -> handler 槽位索引 */
    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(beSlot: Int, x: Int, y: Int, spec: SlotSpec) {
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlot] = handlerIndex
        addSlot(PredicateSlot(blockInventory, beSlot, x, y, spec))
    }

    init {
        checkSize(blockInventory, FermenterBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        val inputFilledSpec = itemStorage?.deriveSlotSpec(FermenterBlockEntity.SLOT_INPUT_FILLED_CONTAINER) ?: INPUT_FILLED_CONTAINER_SLOT_SPEC
        val inputEmptySpec = itemStorage?.deriveSlotSpec(FermenterBlockEntity.SLOT_INPUT_EMPTY_CONTAINER) ?: OUTPUT_ONLY_SLOT_SPEC
        val outputEmptySpec = itemStorage?.deriveSlotSpec(FermenterBlockEntity.SLOT_OUTPUT_EMPTY_CONTAINER) ?: OUTPUT_EMPTY_CONTAINER_SLOT_SPEC
        val outputFilledSpec = itemStorage?.deriveSlotSpec(FermenterBlockEntity.SLOT_OUTPUT_FILLED_CONTAINER) ?: OUTPUT_ONLY_SLOT_SPEC

        addTrackedSlot(FermenterBlockEntity.SLOT_INPUT_FILLED_CONTAINER, 0, 0, inputFilledSpec)
        addTrackedSlot(FermenterBlockEntity.SLOT_INPUT_EMPTY_CONTAINER, 0, 0, inputEmptySpec)
        addTrackedSlot(FermenterBlockEntity.SLOT_OUTPUT_EMPTY_CONTAINER, 0, 0, outputEmptySpec)
        addTrackedSlot(FermenterBlockEntity.SLOT_OUTPUT_FILLED_CONTAINER, 0, 0, outputFilledSpec)

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                FermenterBlockEntity.SLOT_UPGRADE_INDICES[i],
                0,
                0,
                upgradeSlotSpec
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasStack()) return stack
        val inSlot = slot.stack
        stack = inSlot.copy()

        when (index) {
            SLOT_INPUT_EMPTY_CONTAINER_INDEX,
            SLOT_OUTPUT_FILLED_CONTAINER_INDEX,
            in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            in PLAYER_INV_START..HOTBAR_END -> {
                val inputFilledSpec = itemStorage?.deriveSlotSpec(FermenterBlockEntity.SLOT_INPUT_FILLED_CONTAINER) ?: INPUT_FILLED_CONTAINER_SLOT_SPEC
                val outputEmptySpec = itemStorage?.deriveSlotSpec(FermenterBlockEntity.SLOT_OUTPUT_EMPTY_CONTAINER) ?: OUTPUT_EMPTY_CONTAINER_SLOT_SPEC
                val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map { SlotTarget(slots[it], upgradeSlotSpec) }
                val moved = SlotMoveHelper.insertIntoTargets(
                    inSlot,
                    listOf(
                        SlotTarget(slots[SLOT_INPUT_FILLED_CONTAINER_INDEX], inputFilledSpec),
                        SlotTarget(slots[SLOT_OUTPUT_EMPTY_CONTAINER_INDEX], outputEmptySpec)
                    ) + upgradeTargets
                )
                if (!moved) return ItemStack.EMPTY
            }
            else -> if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
        }

        if (inSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (inSlot.count == stack.count) return ItemStack.EMPTY
        slot.onTakeItem(player, inSlot)
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is FermenterBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_INPUT_FILLED_CONTAINER_INDEX = 0
        const val SLOT_INPUT_EMPTY_CONTAINER_INDEX = 1
        const val SLOT_OUTPUT_EMPTY_CONTAINER_INDEX = 2
        const val SLOT_OUTPUT_FILLED_CONTAINER_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 43

        // 客户端 fallback SlotSpec
        private val biomassCellId = Identifier("ic2_120", "biomass_cell")
        private val emptyCellId = Identifier("ic2_120", "empty_cell")

        private val INPUT_FILLED_CONTAINER_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                when {
                    stack.item == ModFluids.BIOMASS_BUCKET -> true
                    Registries.ITEM.getId(stack.item) == biomassCellId -> true
                    stack.item is FluidCellItem -> {
                        val fluid = stack.getFluidCellVariant()?.fluid
                        fluid == ModFluids.BIOMASS_STILL || fluid == ModFluids.BIOMASS_FLOWING
                    }
                    else -> false
                }
            }
        )

        private val OUTPUT_EMPTY_CONTAINER_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item == Items.BUCKET ||
                    Registries.ITEM.getId(stack.item) == emptyCellId ||
                    (stack.item is FluidCellItem && stack.isFluidCellEmpty())
            }
        )

        private val OUTPUT_ONLY_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): FermenterScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(FermenterBlockEntity.INVENTORY_SIZE)
            return FermenterScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

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
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.FermenterSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Container
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.resources.ResourceLocation

@ModScreenHandler(block = FermenterBlock::class, inventorySize = FermenterBlockEntity.INVENTORY_SIZE)
class FermenterScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    private val blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(FermenterScreenHandler::class.type(), syncId) {

    val sync = FermenterSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

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
        val outputFertilizerSpec = itemStorage?.deriveSlotSpec(FermenterBlockEntity.SLOT_OUTPUT_FERTILIZER) ?: OUTPUT_ONLY_SLOT_SPEC

        addTrackedSlot(FermenterBlockEntity.SLOT_INPUT_FILLED_CONTAINER, 13, 45, inputFilledSpec)
        addTrackedSlot(FermenterBlockEntity.SLOT_INPUT_EMPTY_CONTAINER, 13, 63, inputEmptySpec)
        addTrackedSlot(FermenterBlockEntity.SLOT_OUTPUT_EMPTY_CONTAINER, 147, 42, outputEmptySpec)
        addTrackedSlot(FermenterBlockEntity.SLOT_OUTPUT_FILLED_CONTAINER, 147, 60, outputFilledSpec)
        addTrackedSlot(FermenterBlockEntity.SLOT_OUTPUT_FERTILIZER, 85, 82, outputFertilizerSpec)

        val upgradeX = intArrayOf(133, 151)
        val upgradeY = intArrayOf(82, 82)
        for (i in 0 until 2) {
            addTrackedSlot(
                FermenterBlockEntity.SLOT_UPGRADE_INDICES[i],
                upgradeX[i],
                upgradeY[i],
                upgradeSlotSpec
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 7 + col * 18, 101 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 7 + col * 18, 159))
        }
    }

    override fun quickMove(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasStack()) return stack
        val inSlot = slot.stack
        stack = inSlot.copy()

        when (index) {
            SLOT_INPUT_EMPTY_CONTAINER_INDEX,
            SLOT_OUTPUT_FILLED_CONTAINER_INDEX,
            SLOT_OUTPUT_FERTILIZER_INDEX,
            in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            in PLAYER_INV_START..HOTBAR_END -> {
                val storage = itemStorage ?: return ItemStack.EMPTY
                val moved = SlotMoveHelper.insertFromRoutes(
                    inSlot,
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
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY
            }
        }

        if (inSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (inSlot.count == stack.count) return ItemStack.EMPTY
        slot.onTakeItem(player, inSlot)
        return stack
    }

    override fun canUse(player: Player): Boolean =
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
        const val SLOT_OUTPUT_FERTILIZER_INDEX = 4
        const val SLOT_UPGRADE_INDEX_START = 5
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 42

        private val biomassCellId = ResourceLocation.of("ic2_120", "biomass_cell")
        private val emptyCellId = ResourceLocation.of("ic2_120", "empty_cell")

        private val INPUT_FILLED_CONTAINER_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                when {
                    stack.item == ModFluids.BIOMASS_BUCKET -> true
                    BuiltInRegistries.ITEM.getId(stack.item) == biomassCellId -> true
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
                    BuiltInRegistries.ITEM.getId(stack.item) == emptyCellId ||
                    (stack.item is FluidCellItem && stack.isFluidCellEmpty())
            }
        )

        private val OUTPUT_ONLY_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

    }
}

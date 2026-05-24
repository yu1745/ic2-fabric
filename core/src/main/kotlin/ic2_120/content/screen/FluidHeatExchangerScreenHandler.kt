package ic2_120.content.screen

import ic2_120.content.block.FluidHeatExchangerBlock
import ic2_120.content.block.machines.FluidHeatExchangerBlockEntity
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.FluidHeatExchangerSync
import ic2_120.content.sync.HeatFlowSync
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

@ModScreenHandler(block = FluidHeatExchangerBlock::class)
class FluidHeatExchangerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(FluidHeatExchangerScreenHandler::class.type(), syncId) {

    private val syncedView = SyncedDataView(propertyDelegate)
    private val heatFlow = HeatFlowSync(
        syncedView,
        object : HeatFlowSync.HeatProducer {
            override fun getLastGeneratedHeat(): Long = 0L
            override fun getLastOutputHeat(): Long = 0L
        }
    )
    val sync = FluidHeatExchangerSync(syncedView, heatFlow)

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
        checkSize(blockInventory, FluidHeatExchangerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 交换器槽 第一行 (46,50) 五个横排无间隔
        for (col in 0 until 5) {
            val slotIndex = FluidHeatExchangerBlockEntity.SLOT_EXCHANGER_INDICES[col]
            val spec = itemStorage?.deriveSlotSpec(slotIndex) ?: EXCHANGER_SLOT_SPEC
            addTrackedSlot(slotIndex, 46 + col * 17, 50, spec)
        }
        // 交换器槽 第二行 (46,72) 五个横排无间隔
        for (col in 0 until 5) {
            val slotIndex = FluidHeatExchangerBlockEntity.SLOT_EXCHANGER_INDICES[col + 5]
            val spec = itemStorage?.deriveSlotSpec(slotIndex) ?: EXCHANGER_SLOT_SPEC
            addTrackedSlot(slotIndex, 46 + col * 17, 72, spec)
        }

        // 升级槽 3个 (62,103) 横排无间隔
        for (i in 0 until 3) {
            addTrackedSlot(FluidHeatExchangerBlockEntity.SLOT_UPGRADE_INDICES[i], 62 + i * 18, 103, upgradeSlotSpec)
        }

        // 容器槽
        val inputFilledSpec = itemStorage?.deriveSlotSpec(FluidHeatExchangerBlockEntity.SLOT_INPUT_FILLED_CONTAINER) ?: INPUT_FILLED_CONTAINER_SLOT_SPEC
        val inputEmptySpec = itemStorage?.deriveSlotSpec(FluidHeatExchangerBlockEntity.SLOT_INPUT_EMPTY_CONTAINER) ?: OUTPUT_ONLY_SLOT_SPEC
        val outputEmptySpec = itemStorage?.deriveSlotSpec(FluidHeatExchangerBlockEntity.SLOT_OUTPUT_EMPTY_CONTAINER) ?: OUTPUT_EMPTY_CONTAINER_SLOT_SPEC
        val outputFilledSpec = itemStorage?.deriveSlotSpec(FluidHeatExchangerBlockEntity.SLOT_OUTPUT_FILLED_CONTAINER) ?: OUTPUT_ONLY_SLOT_SPEC

        addTrackedSlot(FluidHeatExchangerBlockEntity.SLOT_INPUT_FILLED_CONTAINER, 8, 103, inputFilledSpec)
        addTrackedSlot(FluidHeatExchangerBlockEntity.SLOT_INPUT_EMPTY_CONTAINER, 26, 103, inputEmptySpec)
        addTrackedSlot(FluidHeatExchangerBlockEntity.SLOT_OUTPUT_EMPTY_CONTAINER, 134, 103, outputEmptySpec)
        addTrackedSlot(FluidHeatExchangerBlockEntity.SLOT_OUTPUT_FILLED_CONTAINER, 152, 103, outputFilledSpec)

        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18))
            }
        }
        // 快捷栏
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 180))
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
            SLOT_OUTPUT_FILLED_CONTAINER_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }

            in EXCHANGER_SLOT_INDEX_START..EXCHANGER_SLOT_INDEX_END,
            in SLOT_UPGRADE_START..SLOT_UPGRADE_END,
            SLOT_INPUT_FILLED_CONTAINER_INDEX,
            SLOT_OUTPUT_EMPTY_CONTAINER_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }

            in PLAYER_INV_START..HOTBAR_END -> {
                val storage = itemStorage ?: return ItemStack.EMPTY
                val moved = SlotMoveHelper.insertFromRoutes(
                    inSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots
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
            world.getBlockState(pos).block is FluidHeatExchangerBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val EXCHANGER_SLOT_INDEX_START = 0
        const val EXCHANGER_SLOT_INDEX_END = 9
        const val SLOT_UPGRADE_START = 10
        const val SLOT_UPGRADE_END = 12
        const val SLOT_INPUT_FILLED_CONTAINER_INDEX = 13
        const val SLOT_INPUT_EMPTY_CONTAINER_INDEX = 14
        const val SLOT_OUTPUT_EMPTY_CONTAINER_INDEX = 15
        const val SLOT_OUTPUT_FILLED_CONTAINER_INDEX = 16
        const val PLAYER_INV_START = 17
        const val HOTBAR_END = 52

        private val EXCHANGER_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack ->
                !stack.isEmpty && net.minecraft.registry.Registries.ITEM.getId(stack.item) == net.minecraft.util.Identifier.of("ic2_120", "heat_conductor")
            }
        )

        private val INPUT_FILLED_CONTAINER_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                when {
                    stack.item == net.minecraft.item.Items.LAVA_BUCKET -> true
                    stack.item == ModFluids.HOT_COOLANT_BUCKET -> true
                    net.minecraft.registry.Registries.ITEM.getId(stack.item) == net.minecraft.util.Identifier.of("ic2_120", "lava_cell") -> true
                    net.minecraft.registry.Registries.ITEM.getId(stack.item) == net.minecraft.util.Identifier.of("ic2_120", "hot_coolant_cell") -> true
                    stack.item is FluidCellItem -> {
                        val fluid = stack.getFluidCellVariant()?.fluid
                        fluid == net.minecraft.fluid.Fluids.LAVA ||
                            fluid == net.minecraft.fluid.Fluids.FLOWING_LAVA ||
                            fluid == ModFluids.HOT_COOLANT_STILL ||
                            fluid == ModFluids.HOT_COOLANT_FLOWING
                    }
                    else -> false
                }
            }
        )

        private val OUTPUT_EMPTY_CONTAINER_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item == net.minecraft.item.Items.BUCKET ||
                    net.minecraft.registry.Registries.ITEM.getId(stack.item) == net.minecraft.util.Identifier.of("ic2_120", "empty_cell") ||
                    (stack.item is FluidCellItem && stack.isFluidCellEmpty())
            }
        )

        private val OUTPUT_ONLY_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): FluidHeatExchangerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(FluidHeatExchangerBlockEntity.INVENTORY_SIZE)
            return FluidHeatExchangerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

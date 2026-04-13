package ic2_120.content.screen

import ic2_120.Ic2_120
import ic2_120.content.block.AnimalmatronBlock
import ic2_120.content.block.machines.AnimalmatronBlockEntity
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.AnimalmatronSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.instance
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
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

@ModScreenHandler(block = AnimalmatronBlock::class)
class AnimalmatronScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(AnimalmatronScreenHandler::class.type(), syncId) {

    val sync = AnimalmatronSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, AnimalmatronBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WATER_INPUT, 0, 0, WATER_INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WATER_OUTPUT, 0, 0, OUTPUT_ONLY_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WEED_EX_INPUT, 0, 0, WEED_EX_INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, AnimalmatronBlockEntity.SLOT_WEED_EX_OUTPUT, 0, 0, OUTPUT_ONLY_SLOT_SPEC))

        for ((_, slotIndex) in AnimalmatronBlockEntity.SLOT_FEED_INDICES.withIndex()) {
            addSlot(PredicateSlot(blockInventory, slotIndex, 0, 0, FEED_SLOT_SPEC))
        }

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    AnimalmatronBlockEntity.SLOT_UPGRADE_INDICES[i],
                    0,
                    0,
                    upgradeSlotSpec
                )
            )
        }
        addSlot(PredicateSlot(blockInventory, AnimalmatronBlockEntity.SLOT_DISCHARGING, 0, 0, DISCHARGING_SLOT_SPEC))

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

        when {
            index == SLOT_WATER_OUTPUT_INDEX || index == SLOT_WEED_EX_OUTPUT_INDEX || index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            index in SLOT_FEED_INDEX_START..SLOT_FEED_INDEX_END -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            index == SLOT_DISCHARGING_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY
            }
            index in PLAYER_INV_START..HOTBAR_END -> {
                val feedTargets = (SLOT_FEED_INDEX_START..SLOT_FEED_INDEX_END).map {
                    SlotTarget(slots[it], FEED_SLOT_SPEC)
                }
                val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                    SlotTarget(slots[it], upgradeSlotSpec)
                }
                val moved = SlotMoveHelper.insertIntoTargets(
                    inSlot,
                    listOf(
                        SlotTarget(slots[SLOT_WATER_INPUT_INDEX], WATER_INPUT_SLOT_SPEC),
                        SlotTarget(slots[SLOT_WEED_EX_INPUT_INDEX], WEED_EX_INPUT_SLOT_SPEC),
                        SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC)
                    ) + feedTargets + upgradeTargets
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
            world.getBlockState(pos).block is AnimalmatronBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_WATER_INPUT_INDEX = 0
        const val SLOT_WATER_OUTPUT_INDEX = 1
        const val SLOT_WEED_EX_INPUT_INDEX = 2
        const val SLOT_WEED_EX_OUTPUT_INDEX = 3

        const val SLOT_FEED_INDEX_START = 4
        const val SLOT_FEED_INDEX_END = 10

        const val SLOT_UPGRADE_INDEX_START = 11
        const val SLOT_UPGRADE_INDEX_END = 14
        const val SLOT_DISCHARGING_INDEX = 15

        const val PLAYER_INV_START = 16
        const val HOTBAR_END = 51

        private val fluidCellId = Identifier(Ic2_120.MOD_ID, "fluid_cell")
        private val waterCellId = Identifier(Ic2_120.MOD_ID, "water_cell")
        private val distilledWaterCellId = Identifier(Ic2_120.MOD_ID, "distilled_water_cell")
        private val weedExCellId = Identifier(Ic2_120.MOD_ID, "weed_ex_cell")

        private val WATER_INPUT_SLOT_SPEC = SlotSpec(canInsert = { stack ->
            when {
                stack.item == Items.WATER_BUCKET || stack.item == ModFluids.DISTILLED_WATER_BUCKET -> true
                Registries.ITEM.getId(stack.item) == waterCellId -> true
                Registries.ITEM.getId(stack.item) == distilledWaterCellId -> true
                Registries.ITEM.getId(stack.item) == fluidCellId && stack.item is FluidCellItem -> {
                    val fluid = stack.getFluidCellVariant()?.fluid
                    fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
                        fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING
                }
                else -> false
            }
        })

        private val WEED_EX_INPUT_SLOT_SPEC = SlotSpec(canInsert = { stack ->
            when {
                stack.item == ModFluids.WEED_EX_BUCKET -> true
                Registries.ITEM.getId(stack.item) == weedExCellId -> true
                Registries.ITEM.getId(stack.item) == fluidCellId && stack.item is FluidCellItem -> {
                    val fluid = stack.getFluidCellVariant()?.fluid
                    fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING
                }
                else -> false
            }
        })

        private val FEED_SLOT_SPEC = SlotSpec(canInsert = { stack ->
            when (stack.item) {
                Items.CARROT, Items.WHEAT, Items.WHEAT_SEEDS, Items.DANDELION,
                Items.GOLDEN_APPLE, Items.GOLDEN_CARROT, Items.HAY_BLOCK -> true
                else -> false
            }
        })

        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item is IBatteryItem },
            maxItemCount = 1
        )

        private val OUTPUT_ONLY_SLOT_SPEC = SlotSpec(canInsert = { false }, canTake = { true })

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): AnimalmatronScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(AnimalmatronBlockEntity.INVENTORY_SIZE)
            return AnimalmatronScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

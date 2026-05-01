package ic2_120.content.screen

import ic2_120.content.block.ElectricHeatGeneratorBlock
import ic2_120.content.block.machines.ElectricHeatGeneratorBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = ElectricHeatGeneratorBlock::class)
class ElectricHeatGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ElectricHeatGeneratorScreenHandler::class.type(), syncId) {

    private val syncedView = SyncedDataView(propertyDelegate)
    private val heatFlow = HeatFlowSync(
        syncedView,
        object : HeatFlowSync.HeatProducer {
            override fun getLastGeneratedHeat(): Long = 0L
            override fun getLastOutputHeat(): Long = 0L
        }
    )
    val sync = ElectricHeatGeneratorSync(syncedView, heatFlow = heatFlow)
    private val coilItem = Registries.ITEM.get(Identifier.of("ic2_120", "coil"))

    init {
        checkSize(blockInventory, ElectricHeatGeneratorBlockEntity.SLOT_COUNT)
        addProperties(propertyDelegate)

        repeat(ElectricHeatGeneratorBlockEntity.SLOT_DISCHARGING) { i ->
            addSlot(PredicateSlot(blockInventory, i, 0, 0, COIL_SLOT_SPEC))
        }
        addSlot(PredicateSlot(blockInventory, ElectricHeatGeneratorBlockEntity.SLOT_DISCHARGING, 0, 0, DISCHARGING_SLOT_SPEC))

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
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in 0 until ElectricHeatGeneratorBlockEntity.SLOT_DISCHARGING -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val moved = when {
                        stackInSlot.item is IBatteryItem -> {
                            SlotMoveHelper.insertIntoTargets(
                                stackInSlot,
                                listOf(SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC))
                            )
                        }
                        stackInSlot.item == coilItem -> {
                            val coilTargets = (0 until ElectricHeatGeneratorBlockEntity.SLOT_DISCHARGING).map {
                                SlotTarget(slots[it], COIL_SLOT_SPEC)
                            }
                            SlotMoveHelper.insertIntoTargets(stackInSlot, coilTargets)
                        }
                        else -> false
                    }
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
            world.getBlockState(pos).block is ElectricHeatGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_DISCHARGING_INDEX = ElectricHeatGeneratorBlockEntity.SLOT_DISCHARGING
        const val PLAYER_INV_START = ElectricHeatGeneratorBlockEntity.SLOT_COUNT
        const val HOTBAR_END = PLAYER_INV_START + 36

        private val COIL_SLOT_SPEC = SlotSpec(maxItemCount = 1, canInsert = { stack ->
            !stack.isEmpty && stack.item == Registries.ITEM.get(Identifier.of("ic2_120", "coil"))
        })
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ElectricHeatGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(ElectricHeatGeneratorBlockEntity.SLOT_COUNT)
            return ElectricHeatGeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}


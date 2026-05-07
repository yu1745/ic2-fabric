package ic2_120_advanced_solar_addon.content.screen

import ic2_120_advanced_solar_addon.content.sync.SolarPanelSync
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
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
import net.minecraft.util.math.Direction

@ModScreenHandler(names = ["advanced_solar_panel", "hybrid_solar_panel", "ultimate_solar_panel", "quantum_solar_panel"])
class SolarPanelScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val chargeInventory: Inventory? = null,
    private val chargeSlotCount: Int = 0,
    private val chargeTier: Int = 1
) : ScreenHandler(SolarPanelScreenHandler::class.type(), syncId) {

    val sync = SolarPanelSync(
        schema = SyncedDataView(propertyDelegate),
        capacity = 1L,
        tier = 1,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    val machineSlotCount: Int get() = chargeSlotCount

    init {
        addProperties(propertyDelegate)

        // Charge slots
        if (chargeInventory != null && chargeSlotCount > 0) {
            checkSize(chargeInventory, chargeSlotCount)
            for (i in 0 until chargeSlotCount) {
                addSlot(PredicateSlot(chargeInventory, i, 0, 0, chargeSlotSpec(chargeTier)))
            }
        }

        // Player inventory
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
        if (index !in slots.indices) return ItemStack.EMPTY

        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            val movedFromMachine = index in 0 until machineSlotCount
            val moved = when {
                movedFromMachine -> {
                    if (!insertItem(stackInSlot, machineSlotCount, machineSlotCount + 36, true)) {
                        false
                    } else {
                        slot.onQuickTransfer(stackInSlot, stack)
                        true
                    }
                }
                index >= machineSlotCount -> {
                    SlotMoveHelper.insertIntoTargets(stackInSlot, machineSlotTargets())
                }
                else -> false
            }

            if (!moved) return ItemStack.EMPTY
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    private fun machineSlotTargets(): List<SlotTarget> {
        if (chargeSlotCount == 0) return emptyList()
        return (0 until chargeSlotCount).map { i ->
            SlotTarget(slots[i], chargeSlotSpec(chargeTier))
        }
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        private fun chargeSlotSpec(maxTier: Int): SlotSpec = SlotSpec(
            canInsert = { stack ->
                val item = stack.item
                (item is IBatteryItem || item is IElectricTool) && item.tier <= maxTier
            },
            maxItemCount = 1
        )

        @ScreenFactory
        @JvmStatic
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SolarPanelScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val chargeSlots = buf.readVarInt()
            val tier = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val propertyDelegate = ArrayPropertyDelegate(propertyCount)
            val chargeInv = if (chargeSlots > 0) SimpleInventory(chargeSlots) else null
            return SolarPanelScreenHandler(syncId, playerInventory, context, propertyDelegate, chargeInv, chargeSlots, tier)
        }
    }
}

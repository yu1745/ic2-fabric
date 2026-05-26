package ic2_120_advanced_solar_addon.content.screen

import ic2_120_advanced_solar_addon.content.sync.SolarPanelSync
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.storage.RoutedItemStorage
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
    private val chargeTier: Int = 1,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(SolarPanelScreenHandler::class.type(), syncId) {

    val sync = SolarPanelSync(
        schema = SyncedDataView(propertyDelegate),
        capacity = 1L,
        tier = 1,
        getFacing = { Direction.NORTH },
        currentTickProvider = { null }
    )

    val machineSlotCount: Int get() = chargeSlotCount

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private val hotbarEnd: Int get() = machineSlotCount + 36

    init {
        addProperties(propertyDelegate)

        // Charge slots: x=17 + i*18, y=59
        if (chargeInventory != null && chargeSlotCount > 0) {
            checkSize(chargeInventory, chargeSlotCount)
            for (i in 0 until chargeSlotCount) {
                addTrackedSlot(chargeInventory, i, CHARGE_SLOT_X + i * SLOT_SIZE, CHARGE_SLOT_Y)
            }
        }

        // Player inventory: x=17 + col*18, y=86 + row*18
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * SLOT_SIZE, PLAYER_INV_Y + row * SLOT_SIZE))
            }
        }
        // Hotbar: x=17 + col*18, y=144
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * SLOT_SIZE, HOTBAR_Y))
        }
    }

    private fun addTrackedSlot(inventory: Inventory, beSlotIndex: Int, x: Int, y: Int) {
        val spec = itemStorage?.deriveSlotSpec(beSlotIndex) ?: chargeSlotSpec(chargeTier)
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlotIndex] = handlerIndex
        addSlot(PredicateSlot(inventory, beSlotIndex, x, y, spec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY

        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()

            val beSlot = (slot as? PredicateSlot)?.index ?: -1

            when {
                // Machine charge slots -> player inventory
                beSlot >= 0 -> {
                    if (!insertItem(stackInSlot, machineSlotCount, hotbarEnd, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // Player inventory -> machine (via RoutedItemStorage routes)
                index in machineSlotCount until hotbarEnd -> {
                    val storage = itemStorage
                    if (storage != null) {
                        val moved = SlotMoveHelper.insertFromRoutes(
                            stackInSlot,
                            storage,
                            storage.insertRoutes,
                            beSlotToHandlerIndex,
                            slots
                        )
                        if (!moved) return ItemStack.EMPTY
                    } else {
                        return ItemStack.EMPTY
                    }
                }
                // Hotbar <-> main inventory swap
                else -> {
                    if (!insertItem(stackInSlot, machineSlotCount, hotbarEnd, false)) return ItemStack.EMPTY
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
            player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        // Charge slots: start at 17,59, horizontal no gap
        private const val CHARGE_SLOT_X = 17
        private const val CHARGE_SLOT_Y = 59

        // Player inventory: start at 17,86
        private const val PLAYER_INV_X = 17
        private const val PLAYER_INV_Y = 86

        // Hotbar: y=144
        private const val HOTBAR_Y = 144

        private fun chargeSlotSpec(maxTier: Int): SlotSpec = SlotSpec(
            canInsert = { stack ->
                val item = stack.item
                when {
                    item is IElectricTool -> true
                    item is IBatteryItem && item.canCharge -> item.tier <= maxTier
                    else -> false
                }
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

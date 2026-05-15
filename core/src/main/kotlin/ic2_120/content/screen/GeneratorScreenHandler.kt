package ic2_120.content.screen

import ic2_120.content.sync.GeneratorSync
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = GeneratorBlock::class)
class GeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(GeneratorScreenHandler::class.type(), syncId) {

    val sync = GeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(slot: Slot, beSlotIndex: Int) {
        beSlotToHandlerIndex[beSlotIndex] = slots.size
        addSlot(slot)
    }

    init {
        checkSize(blockInventory, 2)
        addProperties(propertyDelegate)

        // 机器槽位（规则由 RoutedItemStorage 路由推导）
        val fuelSlotSpec = itemStorage?.deriveSlotSpec(MachineBlockEntity.FUEL_SLOT)
            ?: SLOT_SPEC_FALLBACK_FUEL
        val batterySlotSpec = itemStorage?.deriveSlotSpec(MachineBlockEntity.BATTERY_SLOT)
            ?: SLOT_SPEC_FALLBACK_BATTERY

        addTrackedSlot(PredicateSlot(blockInventory, MachineBlockEntity.FUEL_SLOT, 0, 0, fuelSlotSpec), MachineBlockEntity.FUEL_SLOT)
        addTrackedSlot(PredicateSlot(blockInventory, MachineBlockEntity.BATTERY_SLOT, 0, 0, batterySlotSpec), MachineBlockEntity.BATTERY_SLOT)

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
                // 燃料槽 -> 玩家物品栏
                index == SLOT_FUEL_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                // 电池槽 -> 玩家物品栏
                index == SLOT_BATTERY_INDEX -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                // 玩家物品栏 -> 机器（路由驱动）
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val moved = if (itemStorage != null) {
                        SlotMoveHelper.insertFromRoutes(stackInSlot, itemStorage, beSlotToHandlerIndex, slots)
                    } else {
                        SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_FUEL_INDEX], SLOT_SPEC_FALLBACK_FUEL),
                                SlotTarget(slots[SLOT_BATTERY_INDEX], SLOT_SPEC_FALLBACK_BATTERY)
                            )
                        )
                    }
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is GeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_FUEL_INDEX = 0
        const val SLOT_BATTERY_INDEX = 1
        const val PLAYER_INV_START = 2
        const val HOTBAR_END = 37
        const val SLOT_SIZE = 18

        private val SLOT_SPEC_FALLBACK_FUEL = SlotSpec(
            canInsert = { stack ->
                !stack.isEmpty && ((net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.get(stack.item) ?: 0) > 0)
            }
        )
        private val SLOT_SPEC_FALLBACK_BATTERY = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.canBeCharged() }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): GeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(2)
            return GeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

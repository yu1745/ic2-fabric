package ic2_120.content.screen

import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity
import ic2_120.content.block.machines.isSemifluidFuel
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.SemifluidGeneratorSync
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

@ModScreenHandler(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(SemifluidGeneratorScreenHandler::class.type(), syncId) {

    val sync = SemifluidGeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, SemifluidGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, SemifluidGeneratorBlockEntity.FUEL_SLOT, 0, 0, FUEL_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, 0, 0, EMPTY_CONTAINER_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SemifluidGeneratorBlockEntity.BATTERY_SLOT, 0, 0, BATTERY_SLOT_SPEC))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    SemifluidGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i],
                    0,
                    0,
                    upgradeSlotSpec
                )
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
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == SemifluidGeneratorBlockEntity.FUEL_SLOT -> if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                index == SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT -> if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                index == SemifluidGeneratorBlockEntity.BATTERY_SLOT -> if (!insertItem(stackInSlot, 3, 39, true)) return ItemStack.EMPTY
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START..HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                        SlotTarget(slots[it], upgradeSlotSpec)
                    }
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SemifluidGeneratorBlockEntity.FUEL_SLOT], FUEL_SLOT_SPEC),
                            SlotTarget(slots[SemifluidGeneratorBlockEntity.BATTERY_SLOT], BATTERY_SLOT_SPEC)
                        ) + upgradeTargets
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 3, 39, false)) return ItemStack.EMPTY
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
            world.getBlockState(pos).block is SemifluidGeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        private val FUEL_SLOT_SPEC = SlotSpec(
            maxItemCount = 64,
            canInsert = { stack -> stack.isSemifluidFuel() }
        )
        private val EMPTY_CONTAINER_SLOT_SPEC = SlotSpec(maxItemCount = 64, canInsert = { false })
        private val BATTERY_SLOT_SPEC = SlotSpec(maxItemCount = 1, canInsert = { stack -> stack.canBeCharged() })

        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SemifluidGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(SemifluidGeneratorBlockEntity.INVENTORY_SIZE)
            return SemifluidGeneratorScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

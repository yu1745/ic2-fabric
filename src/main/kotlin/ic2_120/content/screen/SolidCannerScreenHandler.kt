package ic2_120.content.screen

import ic2_120.content.block.SolidCannerBlock
import ic2_120.content.block.machines.SolidCannerBlockEntity
import ic2_120.content.item.EmptyFuelRodItem
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.recipes.solidcanner.SolidCannerRecipe
import ic2_120.content.sync.SolidCannerSync
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
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = SolidCannerBlock::class)
class SolidCannerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(SolidCannerScreenHandler::class.type(), syncId) {

    val sync = SolidCannerSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    private val tinCanItem by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "tin_can")) }

    private val containerSlotSpec = SlotSpec(
        canInsert = { stack ->
            !stack.isEmpty && (stack.item == tinCanItem || stack.item is EmptyFuelRodItem)
        }
    )
    private val foodSlotSpec = SlotSpec(
        canInsert = { stack ->
            !stack.isEmpty && stack.item !is IBatteryItem && stack.item !is IUpgradeItem &&
                context.get({ world, _ ->
                    world.recipeManager.values().any { it is SolidCannerRecipe && it.slot1Ingredient.test(stack) }
                }, false)
        }
    )
    private val outputSlotSpec = SlotSpec(canInsert = { false }, canTake = { true })
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    init {
        checkSize(blockInventory, SolidCannerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_TIN_CAN, 0, 0, containerSlotSpec))
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_FOOD, 0, 0, foodSlotSpec))
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_OUTPUT, 0, 0, outputSlotSpec))
        addSlot(PredicateSlot(blockInventory, SolidCannerBlockEntity.SLOT_DISCHARGING, 0, 0, dischargingSlotSpec))
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    SolidCannerBlockEntity.SLOT_UPGRADE_INDICES[i],
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
                index == SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index == SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                index in PLAYER_INV_START until HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                        SlotTarget(slots[it], upgradeSlotSpec)
                    }
                    val dischargingTarget = SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec)
                    val containerTarget = SlotTarget(slots[SLOT_TIN_CAN_INDEX], containerSlotSpec)
                    val foodTarget = SlotTarget(slots[SLOT_FOOD_INDEX], foodSlotSpec)
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(containerTarget, foodTarget, dischargingTarget) + upgradeTargets
                    )
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
            world.getBlockState(pos).block is SolidCannerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val SLOT_SIZE = 18

        const val SLOT_TIN_CAN_INDEX = 0
        const val SLOT_FOOD_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_DISCHARGING_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): SolidCannerScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(SolidCannerBlockEntity.INVENTORY_SIZE)
            return SolidCannerScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

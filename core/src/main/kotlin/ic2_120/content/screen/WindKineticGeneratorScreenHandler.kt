package ic2_120.content.screen

import ic2_120.compat.stack

import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.sync.WindKineticGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(block = WindKineticGeneratorBlock::class, inventorySize = WindKineticGeneratorBlockEntity.INVENTORY_SIZE)
class WindKineticGeneratorScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData
) : AbstractContainerMenu(WindKineticGeneratorScreenHandler::class.type(), syncId) {

    val sync = WindKineticGeneratorSync(SyncedDataView(propertyDelegate))

    init {
        checkContainerSize(blockInventory, 1)
        addDataSlots(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, WindKineticGeneratorBlockEntity.ROTOR_SLOT, 79, 25, ROTOR_SLOT_SPEC))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 7 + col * 18, 83 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 7 + col * 18, 141))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == WindKineticGeneratorBlockEntity.ROTOR_SLOT -> {
                    if (!moveItemStackTo(stackInSlot, 1, 37, true)) return ItemStack.EMPTY
                }
                index in 1..36 -> {
                    if (!ROTOR_SLOT_SPEC.canInsert(stackInSlot)) return ItemStack.EMPTY
                    val rotorSlot = slots[WindKineticGeneratorBlockEntity.ROTOR_SLOT]
                    if (rotorSlot .hasItem()) return ItemStack.EMPTY
                    val one = stackInSlot.copy()
                    one.count = 1
                    rotorSlot.set(one)
                    rotorSlot.setChanged()
                    stackInSlot.shrink(1)
                }
                else -> if (!moveItemStackTo(stackInSlot, 1, 37, false)) return ItemStack.EMPTY
            }

            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            world.getBlockState(pos).block is WindKineticGeneratorBlock && player.distanceToSqr(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 1
        const val SLOT_SIZE = 18

        private val ALLOWED_ROTORS = setOf("wooden_rotor", "bronze_rotor", "iron_rotor", "steel_rotor", "carbon_rotor")
        private val ROTOR_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack ->
                ALLOWED_ROTORS.contains(BuiltInRegistries.ITEM.getKey(stack.item).path)
            }
        )

    }
}

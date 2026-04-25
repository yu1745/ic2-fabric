package ic2_120.content.screen

import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.sync.WindKineticGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.registry.Registries
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = WindKineticGeneratorBlock::class)
class WindKineticGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(WindKineticGeneratorScreenHandler::class.type(), syncId) {

    val sync = WindKineticGeneratorSync(SyncedDataView(propertyDelegate))

    init {
        checkSize(blockInventory, 1)
        addProperties(propertyDelegate)

        addSlot(PredicateSlot(blockInventory, WindKineticGeneratorBlockEntity.ROTOR_SLOT, 0, 0, ROTOR_SLOT_SPEC))

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
            when {
                index == WindKineticGeneratorBlockEntity.ROTOR_SLOT -> {
                    if (!insertItem(stackInSlot, 1, 37, true)) return ItemStack.EMPTY
                }
                index in 1..36 -> {
                    if (!ROTOR_SLOT_SPEC.canInsert(stackInSlot)) return ItemStack.EMPTY
                    val rotorSlot = slots[WindKineticGeneratorBlockEntity.ROTOR_SLOT]
                    if (rotorSlot.hasStack()) return ItemStack.EMPTY
                    val one = stackInSlot.copy()
                    one.count = 1
                    rotorSlot.stack = one
                    rotorSlot.markDirty()
                    stackInSlot.decrement(1)
                }
                else -> if (!insertItem(stackInSlot, 1, 37, false)) return ItemStack.EMPTY
            }

            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is WindKineticGeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        const val PLAYER_INV_START = 1
        const val SLOT_SIZE = 18

        private val ALLOWED_ROTORS = setOf("wooden_rotor", "iron_rotor", "steel_rotor", "carbon_rotor")
        private val ROTOR_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack ->
                ALLOWED_ROTORS.contains(Registries.ITEM.getId(stack.item).path)
            }
        )

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): WindKineticGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(1)
            return WindKineticGeneratorScreenHandler(
                syncId,
                playerInventory,
                blockInv,
                context,
                net.minecraft.screen.ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}

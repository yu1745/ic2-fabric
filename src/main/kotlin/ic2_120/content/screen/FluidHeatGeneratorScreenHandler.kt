package ic2_120.content.screen

import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.sync.FluidHeatGeneratorSync
import ic2_120.content.sync.HeatFlowSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = FluidHeatGeneratorBlock::class)
class FluidHeatGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(FluidHeatGeneratorScreenHandler::class.type(), syncId) {

    private val syncedView = SyncedDataView(propertyDelegate)
    private val heatFlow = HeatFlowSync(
        syncedView,
        object : HeatFlowSync.HeatProducer {
            override fun getLastGeneratedHeat(): Long = 0L
            override fun getLastOutputHeat(): Long = 0L
        }
    )
    val sync = FluidHeatGeneratorSync(syncedView, heatFlow)

    init {
        checkSize(blockInventory, FluidHeatGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 燃料容器槽
        addSlot(PredicateSlot(blockInventory, FluidHeatGeneratorBlockEntity.FUEL_SLOT, 80, 35, FUEL_SLOT_SPEC))
        // 空容器槽
        addSlot(PredicateSlot(blockInventory, FluidHeatGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, 80, 62, EMPTY_CONTAINER_SLOT_SPEC))

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when {
                index == FluidHeatGeneratorBlockEntity.FUEL_SLOT -> {
                    if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                }
                index == FluidHeatGeneratorBlockEntity.EMPTY_CONTAINER_SLOT -> {
                    if (!insertItem(stackInSlot, 2, 38, true)) return ItemStack.EMPTY
                }
                index in 2..37 -> {
                    if (FUEL_SLOT_SPEC.canInsert(stackInSlot)) {
                        if (!insertItem(stackInSlot, FluidHeatGeneratorBlockEntity.FUEL_SLOT, FluidHeatGeneratorBlockEntity.FUEL_SLOT + 1, false)) return ItemStack.EMPTY
                    } else return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, 2, 38, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is FluidHeatGeneratorBlock &&
                player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)

    companion object {
        private val FUEL_SLOT_SPEC = SlotSpec(canInsert = { stack ->
            !stack.isEmpty && (
                stack.item == Items.LAVA_BUCKET ||
                stack.item == net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier("ic2_120", "biofuel_bucket")) ||
                stack.item == net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier("ic2_120", "biofuel_cell")) ||
                stack.item == net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier("ic2_120", "fluid_cell"))
            )
        })

        private val EMPTY_CONTAINER_SLOT_SPEC = SlotSpec(canInsert = { false })

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): FluidHeatGeneratorScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            return FluidHeatGeneratorScreenHandler(
                syncId,
                playerInventory,
                SimpleInventory(FluidHeatGeneratorBlockEntity.INVENTORY_SIZE),
                context,
                ArrayPropertyDelegate(propertyCount)
            )
        }
    }
}


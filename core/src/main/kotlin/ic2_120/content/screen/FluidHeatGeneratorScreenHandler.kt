package ic2_120.content.screen

import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
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
import ic2_120.registry.annotation.ScreenFactory

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

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, FluidHeatGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 燃料容器槽
        addSlot(PredicateSlot(blockInventory, FluidHeatGeneratorBlockEntity.FUEL_SLOT, 0, 0, FUEL_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, FluidHeatGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, 0, 0, EMPTY_CONTAINER_SLOT_SPEC))

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    FluidHeatGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i],
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
                index == FluidHeatGeneratorBlockEntity.FUEL_SLOT -> {
                    if (!insertItem(stackInSlot, UPGRADE_END + 1, HOTBAR_END + 1, true)) return ItemStack.EMPTY
                }
                index == FluidHeatGeneratorBlockEntity.EMPTY_CONTAINER_SLOT -> {
                    if (!insertItem(stackInSlot, UPGRADE_END + 1, HOTBAR_END + 1, true)) return ItemStack.EMPTY
                }
                index in SLOT_UPGRADE_START..SLOT_UPGRADE_END -> {
                    if (!insertItem(stackInSlot, UPGRADE_END + 1, HOTBAR_END + 1, true)) return ItemStack.EMPTY
                }
                index in (UPGRADE_END + 1)..HOTBAR_END -> {
                    val upgradeTargets = (SLOT_UPGRADE_START..SLOT_UPGRADE_END).map { SlotTarget(slots[it], upgradeSlotSpec) }
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[FluidHeatGeneratorBlockEntity.FUEL_SLOT], FUEL_SLOT_SPEC)
                        ) + upgradeTargets
                    )
                    if (!moved) return ItemStack.EMPTY
                }
                else -> if (!insertItem(stackInSlot, UPGRADE_END + 1, HOTBAR_END + 1, false)) return ItemStack.EMPTY
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
        const val PLAYER_INV_START = 6
        const val SLOT_SIZE = 18

        const val SLOT_UPGRADE_START = 2
        const val SLOT_UPGRADE_END = 5
        const val UPGRADE_END = 5
        const val HOTBAR_END = 43

        private val FUEL_SLOT_SPEC = SlotSpec(canInsert = { stack ->
            !stack.isEmpty && (
                stack.item == Items.LAVA_BUCKET ||
                stack.item == net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier("ic2_120", "biofuel_bucket")) ||
                stack.item == net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier("ic2_120", "biofuel_cell")) ||
                stack.item == net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier("ic2_120", "fluid_cell"))
            )
        })

        private val EMPTY_CONTAINER_SLOT_SPEC = SlotSpec(canInsert = { false })

        @ScreenFactory
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


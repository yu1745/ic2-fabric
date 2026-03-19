package ic2_120.content.screen

import ic2_120.content.item.IBlockCuttingBlade
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.BlockCutterSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.BlockCutterBlock
import ic2_120.content.block.machines.BlockCutterBlockEntity
import ic2_120.registry.annotation.ModScreenHandler
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

@ModScreenHandler(block = BlockCutterBlock::class)
class BlockCutterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(BlockCutterScreenHandler::class.type(), syncId) {

    val sync = BlockCutterSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, BlockCutterBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 1: 原料槽（第二行左）
        addSlot(PredicateSlot(blockInventory, BlockCutterBlockEntity.SLOT_INPUT, INPUT_SLOT_X, INPUT_SLOT_Y, INPUT_SLOT_SPEC))

        // 2: 供电槽（第四行左）
        addSlot(PredicateSlot(
            blockInventory,
            BlockCutterBlockEntity.SLOT_DISCHARGING,
            DISCHARGING_SLOT_X,
            DISCHARGING_SLOT_Y,
            DISCHARGING_SLOT_SPEC
        ))

        // 3: 成品槽（第二行右）
        addSlot(PredicateSlot(blockInventory, BlockCutterBlockEntity.SLOT_OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y, OUTPUT_SLOT_SPEC))

        // 4: 锯片槽（第三行居中，进度条下方）
        addSlot(PredicateSlot(blockInventory, BlockCutterBlockEntity.SLOT_BLADE, BLADE_SLOT_X, BLADE_SLOT_Y, BLADE_SLOT_SPEC))

        // 5: 升级插件槽 x4
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    BlockCutterBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i) + MACHINE_OFFSET_Y,
                    upgradeSlotSpec
                )
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                SLOT_OUTPUT_INDEX, SLOT_INPUT_INDEX, SLOT_BLADE_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                else -> {
                    if (index in PLAYER_INV_START..HOTBAR_END) {
                        val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_BLADE_INDEX], BLADE_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC),
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC)
                            ) + upgradeTargets
                        )
                        if (!moved) return ItemStack.EMPTY
                    } else if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY
                    }
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
            world.getBlockState(pos).block is BlockCutterBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        /** 机器区域整体下移量（为顶行标题+能量留出空间） */
        const val MACHINE_OFFSET_Y = 22

        const val INPUT_SLOT_X = 56
        const val INPUT_SLOT_Y = 17 + MACHINE_OFFSET_Y
        const val DISCHARGING_SLOT_X = 56
        const val DISCHARGING_SLOT_Y = 53 + MACHINE_OFFSET_Y
        const val OUTPUT_SLOT_X = 116
        const val OUTPUT_SLOT_Y = 17 + MACHINE_OFFSET_Y
        /** 锯片槽居中于进度条下方：(176/2 - 18/2) = 79 */
        const val BLADE_SLOT_X = 79
        const val BLADE_SLOT_Y = 35 + MACHINE_OFFSET_Y
        const val SLOT_SIZE = 18

        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        private val BLADE_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBlockCuttingBlade }
        )
        private val INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item !is IBatteryItem && stack.item !is IUpgradeItem && stack.item !is IBlockCuttingBlade }
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        const val SLOT_INPUT_INDEX = 0
        const val SLOT_DISCHARGING_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_BLADE_INDEX = 3
        const val SLOT_UPGRADE_INDEX_START = 4
        const val SLOT_UPGRADE_INDEX_END = 7
        const val PLAYER_INV_START = 8
        const val HOTBAR_END = 44

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): BlockCutterScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(BlockCutterBlockEntity.INVENTORY_SIZE)
            return BlockCutterScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

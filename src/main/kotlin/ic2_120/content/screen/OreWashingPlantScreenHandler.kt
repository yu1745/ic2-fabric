package ic2_120.content.screen

import ic2_120.content.block.OreWashingPlantBlock
import ic2_120.content.block.machines.OreWashingPlantBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.WaterCell
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.sync.OreWashingPlantSync
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

@ModScreenHandler(block = OreWashingPlantBlock::class)
class OreWashingPlantScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(OreWashingPlantScreenHandler::class.type(), syncId) {

    val sync = OreWashingPlantSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, OreWashingPlantBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 矿石输入槽
        addSlot(PredicateSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_INPUT_ORE,
            INPUT_ORE_SLOT_X, INPUT_ORE_SLOT_Y, INPUT_ORE_SLOT_SPEC))

        // 水输入槽
        addSlot(PredicateSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_INPUT_WATER,
            INPUT_WATER_SLOT_X, INPUT_WATER_SLOT_Y, INPUT_WATER_SLOT_SPEC))

        // 三个输出槽（垂直排列）
        addSlot(PredicateSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_1,
            OUTPUT_SLOT_X, OUTPUT_SLOT_Y_1, OUTPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_2,
            OUTPUT_SLOT_X, OUTPUT_SLOT_Y_2, OUTPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_3,
            OUTPUT_SLOT_X, OUTPUT_SLOT_Y_3, OUTPUT_SLOT_SPEC))

        // 空容器输出槽
        addSlot(PredicateSlot(blockInventory, OreWashingPlantBlockEntity.SLOT_OUTPUT_EMPTY,
            EMPTY_OUTPUT_SLOT_X, EMPTY_OUTPUT_SLOT_Y, EMPTY_OUTPUT_SLOT_SPEC))

        // 放电槽（电池）
        addSlot(PredicateSlot(
            blockInventory,
            OreWashingPlantBlockEntity.SLOT_DISCHARGING,
            DISCHARGING_SLOT_X,
            DISCHARGING_SLOT_Y,
            DISCHARGING_SLOT_SPEC
        ))

        // 4 个升级槽
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    OreWashingPlantBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i),
                    upgradeSlotSpec
                )
            )
        }

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }

        // 快捷栏
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
                in SLOT_OUTPUT_1_INDEX..SLOT_OUTPUT_3_INDEX, SLOT_OUTPUT_EMPTY_INDEX -> {
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
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_WATER_INDEX], INPUT_WATER_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_ORE_INDEX], INPUT_ORE_SLOT_SPEC)
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
            world.getBlockState(pos).block is OreWashingPlantBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        // 槽位位置
        const val INPUT_ORE_SLOT_X = 56
        const val INPUT_ORE_SLOT_Y = 35
        const val INPUT_WATER_SLOT_X = 56
        const val INPUT_WATER_SLOT_Y = 53
        const val OUTPUT_SLOT_X = 116
        const val OUTPUT_SLOT_Y_1 = 17   // 纯净的粉碎矿石
        const val OUTPUT_SLOT_Y_2 = 35   // 石粉
        const val OUTPUT_SLOT_Y_3 = 53   // 小撮金属粉
        const val EMPTY_OUTPUT_SLOT_X = 56
        const val EMPTY_OUTPUT_SLOT_Y = 71
        const val DISCHARGING_SLOT_X = 116
        const val DISCHARGING_SLOT_Y = 71
        const val SLOT_SIZE = 18

        // 玩家物品栏
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        // 槽位规则
        private val INPUT_ORE_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                val item = stack.item
                // 不允许电池进入输入槽
                item !is IBatteryItem &&
                // 允许粉碎矿石（根据物品ID判断）
                (item.toString().contains("crushed") ||
                 net.minecraft.registry.Registries.ITEM.getId(item).path.contains("crushed"))
            }
        )
        private val INPUT_WATER_SLOT_SPEC = SlotSpec(
            canInsert = { stack ->
                stack.item == Items.WATER_BUCKET || stack.item is WaterCell
            }
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )
        private val EMPTY_OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        // 槽位索引
        const val SLOT_INPUT_ORE_INDEX = 0
        const val SLOT_INPUT_WATER_INDEX = 1
        const val SLOT_OUTPUT_1_INDEX = 2
        const val SLOT_OUTPUT_2_INDEX = 3
        const val SLOT_OUTPUT_3_INDEX = 4
        const val SLOT_OUTPUT_EMPTY_INDEX = 5
        const val SLOT_DISCHARGING_INDEX = 6
        const val SLOT_UPGRADE_INDEX_START = 7
        const val SLOT_UPGRADE_INDEX_END = 10
        const val PLAYER_INV_START = 11
        const val HOTBAR_END = 47

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): OreWashingPlantScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(OreWashingPlantBlockEntity.INVENTORY_SIZE)
            return OreWashingPlantScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

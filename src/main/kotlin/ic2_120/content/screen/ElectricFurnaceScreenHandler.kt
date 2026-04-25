package ic2_120.content.screen

import ic2_120.content.sync.ElectricFurnaceSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.block.machines.ElectricFurnaceBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.FurnaceOutputSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

import net.minecraft.screen.ScreenHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import ic2_120.registry.annotation.ScreenFactory

/**
 * 电炉 GUI 的 ScreenHandler。
 * 通过 SyncedDataView 按声明顺序自动对齐 index，无需手动指定。
 */
@ModScreenHandler(block = ElectricFurnaceBlock::class, clientInventorySize = ElectricFurnaceBlockEntity.INVENTORY_SIZE)
class ElectricFurnaceScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ElectricFurnaceScreenHandler::class.type(), syncId) {

    val sync = ElectricFurnaceSync(SyncedDataView(propertyDelegate))

    private val inputSlotSpec = SlotSpec(canInsert = { stack -> stack.item !is IBatteryItem })
    private val outputSlotSpec = SlotSpec(canInsert = { false })
    private val dischargingSlotSpec = SlotSpec(
        canInsert = { stack -> stack.item is IBatteryItem },
        maxItemCount = 1
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        SlotSpec(
            canInsert = { stack ->
                if (stack.isEmpty || stack.item !is ic2_120.content.item.IUpgradeItem) return@SlotSpec false
                ic2_120.content.upgrade.UpgradeItemRegistry.canAccept(
                    context.get({ world, pos -> world.getBlockEntity(pos) }, null),
                    stack.item
                )
            }
        )
    }

    init {
        checkSize(blockInventory, ElectricFurnaceBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)
        // 输入槽（左侧）、输出槽（右侧），同一行，留出上方给标题与能量条
        addSlot(PredicateSlot(blockInventory, ElectricFurnaceBlockEntity.SLOT_INPUT, 0, 0, inputSlotSpec))
        addSlot(FurnaceOutputSlot(blockInventory, ElectricFurnaceBlockEntity.SLOT_OUTPUT, 0, 0, outputSlotSpec) {
            context.get({ world, pos ->
                val be = world.getBlockEntity(pos)
                if (be is ElectricFurnaceBlockEntity) be.dropStoredExperience()
            })
        })
        addSlot(PredicateSlot(blockInventory, ElectricFurnaceBlockEntity.SLOT_DISCHARGING, 0, 0, dischargingSlotSpec))

        // 升级槽
        for (i in 0 until UPGRADE_SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    ElectricFurnaceBlockEntity.SLOT_UPGRADE_INDICES[i],
                    0,
                    0,
                    upgradeSlotSpec
                )
            )
        }

        // 玩家背包
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
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SLOT_DISCHARGING_INDEX], dischargingSlotSpec),
                            SlotTarget(slots[SLOT_INPUT_INDEX], inputSlotSpec)
                        ) + upgradeTargets
                    )
                    if (!moved) {
                        return ItemStack.EMPTY
                    }
                }
                else -> if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) return ItemStack.EMPTY
            }
            if (stackInSlot.isEmpty()) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is ElectricFurnaceBlock && player.squaredDistanceTo(
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        private const val UPGRADE_SLOT_COUNT = 4
        const val SLOT_SIZE = 18

        const val SLOT_INPUT_INDEX = 0
        const val SLOT_OUTPUT_INDEX = 1
        const val SLOT_DISCHARGING_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        /** 客户端从 ExtendedScreenHandlerType 创建：从 buf 读取 pos，用临时 Inventory。 */
        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ElectricFurnaceScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(ElectricFurnaceBlockEntity.INVENTORY_SIZE)
            return ElectricFurnaceScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}

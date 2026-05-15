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
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.SemifluidGeneratorSync
import ic2_120.content.syncs.SyncedDataView
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
import ic2_120.registry.annotation.ScreenFactory

@ModScreenHandler(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate,
    private val itemStorage: RoutedItemStorage? = null
) : ScreenHandler(SemifluidGeneratorScreenHandler::class.type(), syncId) {

    val sync = SemifluidGeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.util.math.Direction.NORTH },
        currentTickProvider = { null }
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    /** BE 槽位索引 -> handler 槽位索引 */
    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(beSlot: Int, x: Int, y: Int, spec: SlotSpec) {
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlot] = handlerIndex
        addSlot(PredicateSlot(blockInventory, beSlot, x, y, spec))
    }

    init {
        checkSize(blockInventory, SemifluidGeneratorBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        val fuelSlotSpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.FUEL_SLOT) ?: FUEL_SLOT_SPEC
        val emptyContainerSlotSpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT) ?: EMPTY_CONTAINER_SLOT_SPEC
        val batterySlotSpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.BATTERY_SLOT) ?: BATTERY_SLOT_SPEC

        addTrackedSlot(SemifluidGeneratorBlockEntity.FUEL_SLOT, 0, 0, fuelSlotSpec)
        addTrackedSlot(SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, 0, 0, emptyContainerSlotSpec)
        addTrackedSlot(SemifluidGeneratorBlockEntity.BATTERY_SLOT, 0, 0, batterySlotSpec)

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                SemifluidGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i],
                0,
                0,
                upgradeSlotSpec
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
            when (index) {
                // 输出槽 / 放电槽 -> 玩家物品栏
                SLOT_FUEL_INDEX,
                SLOT_EMPTY_CONTAINER_INDEX,
                SLOT_BATTERY_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 升级槽 -> 玩家物品栏
                in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                else -> {
                    if (index in PLAYER_INV_START..HOTBAR_END) {
                        // 玩家物品栏 -> 机器
                        val fuelSpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.FUEL_SLOT) ?: FUEL_SLOT_SPEC
                        val batterySpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.BATTERY_SLOT) ?: BATTERY_SLOT_SPEC
                        val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_FUEL_INDEX], fuelSpec),
                                SlotTarget(slots[SLOT_BATTERY_INDEX], batterySpec)
                            ) + upgradeTargets
                        )
                        if (!moved) {
                            return ItemStack.EMPTY
                        }
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
            world.getBlockState(pos).block is SemifluidGeneratorBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    /** 客户端读取 BE 上由网络包更新的燃料颜色 */
    val fuelColorArgb: Int get() {
        var color = 0xFFCC4400.toInt()
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is SemifluidGeneratorBlockEntity) {
                color = be.clientFuelColorArgb
            }
            true
        }, true)
        return color
    }

    companion object {
        const val SLOT_SIZE = 18

        // 槽位索引常量（客户端 Screen 引用）
        const val SLOT_FUEL_INDEX = 0
        const val SLOT_EMPTY_CONTAINER_INDEX = 1
        const val SLOT_BATTERY_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        // 客户端 fallback SlotSpec（itemStorage 为 null 时使用）
        private val FUEL_SLOT_SPEC = SlotSpec(
            maxItemCount = 64,
            canInsert = { stack -> stack.isSemifluidFuel() }
        )
        private val EMPTY_CONTAINER_SLOT_SPEC = SlotSpec(maxItemCount = 64, canInsert = { false })
        private val BATTERY_SLOT_SPEC = SlotSpec(maxItemCount = 1, canInsert = { stack -> stack.canBeCharged() })

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

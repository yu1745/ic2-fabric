package ic2_120.content.screen
import ic2_120.compat.blockEntityOrNull

import ic2_120.compat.stack

import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity
import ic2_120.content.block.machines.isSemifluidFuel
import ic2_120.content.item.energy.canBeCharged
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.SemifluidGeneratorSync
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot

@ModScreenHandler(block = SemifluidGeneratorBlock::class, inventorySize = SemifluidGeneratorBlockEntity.INVENTORY_SIZE)
class SemifluidGeneratorScreenHandler(
    syncId: Int,
    playerInventory: Inventory,
    private val blockInventory: Container,
    private val context: ContainerLevelAccess,
    private val propertyDelegate: ContainerData,
    private val itemStorage: RoutedItemStorage? = null
) : AbstractContainerMenu(SemifluidGeneratorScreenHandler::class.type(), syncId) {

    val sync = SemifluidGeneratorSync(
        SyncedDataView(propertyDelegate),
        getFacing = { net.minecraft.core.Direction.NORTH },
        currentTickProvider = { null }
    )

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.blockEntityOrNull() }
    }

    /** BE 槽位索引 -> handler 槽位索引 */
    private val beSlotToHandlerIndex = mutableMapOf<Int, Int>()

    private fun addTrackedSlot(beSlot: Int, x: Int, y: Int, spec: SlotSpec) {
        val handlerIndex = slots.size
        beSlotToHandlerIndex[beSlot] = handlerIndex
        addSlot(PredicateSlot(blockInventory, beSlot, x, y, spec))
    }

    init {
        checkContainerSize(blockInventory, SemifluidGeneratorBlockEntity.INVENTORY_SIZE)
        addDataSlots(propertyDelegate)

        val fuelSlotSpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.FUEL_SLOT) ?: FUEL_SLOT_SPEC
        val emptyContainerSlotSpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT) ?: EMPTY_CONTAINER_SLOT_SPEC
        val batterySlotSpec = itemStorage?.deriveSlotSpec(SemifluidGeneratorBlockEntity.BATTERY_SLOT) ?: BATTERY_SLOT_SPEC

        addTrackedSlot(SemifluidGeneratorBlockEntity.FUEL_SLOT, 38, 18, fuelSlotSpec)
        addTrackedSlot(SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT, 38, 57, emptyContainerSlotSpec)
        addTrackedSlot(SemifluidGeneratorBlockEntity.BATTERY_SLOT, 124, 43, batterySlotSpec)

        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addTrackedSlot(
                SemifluidGeneratorBlockEntity.SLOT_UPGRADE_INDICES[i],
                152,
                4 + i * 20,
                upgradeSlotSpec
            )
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 79 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 137))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot .hasItem()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                // 输出槽 / 放电槽 -> 玩家物品栏
                SLOT_FUEL_INDEX,
                SLOT_EMPTY_CONTAINER_INDEX,
                SLOT_BATTERY_INDEX -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
                }
                // 升级槽 -> 玩家物品栏
                in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stackInSlot, stack)
                }
                else -> {
                    if (index in PLAYER_INV_START until HOTBAR_END) {
                        // 玩家物品栏 -> 机器
                        val storage = itemStorage ?: return ItemStack.EMPTY
                        val moved = SlotMoveHelper.insertFromRoutes(stackInSlot, storage, storage.insertRoutes, beSlotToHandlerIndex, slots)
                        if (!moved) {
                            return ItemStack.EMPTY
                        }
                    } else if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY
                    }
                }
            }
            if (stackInSlot.isEmpty) slot.set(ItemStack.EMPTY)
            else slot.setChanged()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTake(player, stackInSlot)
        }
        return stack
    }

    override fun stillValid(player: Player): Boolean =
        context.evaluate({ world, pos ->
            world.getBlockState(pos).block is SemifluidGeneratorBlock && player.distanceToSqr(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    /** 客户端读取 BE 上由网络包更新的燃料颜色 */
    val fuelColorArgb: Int get() {
        var color = 0xFFCC4400.toInt()
        context.evaluate({ world, pos ->
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

    }
}

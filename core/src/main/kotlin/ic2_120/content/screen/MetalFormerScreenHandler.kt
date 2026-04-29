package ic2_120.content.screen

import ic2_120.content.sync.MetalFormerSync
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.block.machines.MetalFormerBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = MetalFormerBlock::class, clientInventorySize = MetalFormerBlockEntity.INVENTORY_SIZE)
class MetalFormerScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    propertyDelegate: PropertyDelegate
) : ScreenHandler(MetalFormerScreenHandler::class.type(), syncId) {

    val sync = MetalFormerSync(SyncedDataView(propertyDelegate))

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

    /**
     * 服务端处理：收到客户端发来的按钮点击包后在此执行。
     * 客户端应发送 [net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket] 指定 syncId 与 [BUTTON_ID_MODE_CYCLE]。
     */
    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id != BUTTON_ID_MODE_CYCLE) return false
        context.get({ world, pos ->
            val be = world.getBlockEntity(pos)
            if (be is MetalFormerBlockEntity) {
                be.cycleMode()
            }
        }, true)
        return true
    }

    /**
     * 获取方块位置（用于客户端显示或其他需要）
     */
    fun getBlockPos(): net.minecraft.util.math.BlockPos? {
        return context.get({ world, pos -> pos }, null)
    }

    init {
        checkSize(blockInventory, MetalFormerBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 机器槽位
        // Compose 屏幕会在客户端通过 SlotAnchor 回写真实坐标，这里仅放占位坐标。
        addSlot(
            PredicateSlot(
                blockInventory,
                MetalFormerBlockEntity.SLOT_INPUT,
                0,
                0,
                INPUT_SLOT_SPEC
            )
        )

        addSlot(
            PredicateSlot(
                blockInventory,
                MetalFormerBlockEntity.SLOT_DISCHARGING,
                0,
                0,
                DISCHARGING_SLOT_SPEC
            )
        )

        addSlot(
            PredicateSlot(
                blockInventory,
                MetalFormerBlockEntity.SLOT_OUTPUT,
                0,
                0,
                OUTPUT_SLOT_SPEC
            )
        )

        // 升级槽同理使用占位坐标，真实位置由 Compose UI 统一决定。
        for (i in 0 until UPGRADE_SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    MetalFormerBlockEntity.SLOT_UPGRADE_INDICES[i],
                    0,
                    0,
                    upgradeSlotSpec
                )
            )
        }

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }

        // 快捷栏
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
                // 输出槽 -> 玩家物品栏
                SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                // 放电槽 -> 玩家物品栏
                SLOT_DISCHARGING_INDEX -> {
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
                        // 玩家物品栏 -> 机器（放电 -> 输入 -> 升级槽）
                        val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC)
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
            world.getBlockState(pos).block is MetalFormerBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        private const val UPGRADE_SLOT_COUNT = 4
        const val SLOT_SIZE = 18
        private val INPUT_SLOT_SPEC = SlotSpec(
            // 避免电池被误放入加工输入槽，优先进入放电槽。
            canInsert = { stack -> stack.item !is IBatteryItem }
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        // 槽位索引常量
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_DISCHARGING_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        /** 客户端发送按钮点击时使用此 id，服务端在 [onButtonClick] 中处理 */
        const val BUTTON_ID_MODE_CYCLE = 0
    }
}

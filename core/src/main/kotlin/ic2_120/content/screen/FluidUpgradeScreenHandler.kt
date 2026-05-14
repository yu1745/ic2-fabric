package ic2_120.content.screen

import ic2_120.content.item.FluidFilterUpgradeItem
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Hand

/**
 * 流体升级配置 GUI。
 * 适用于 [FluidFilterUpgradeItem]（流体弹出升级和流体抽入升级共用）。
 *
 * 布局：
 * - Slot 0：容器槽位（玩家放入桶/单元以检测流体）
 * - 玩家背包及快捷栏
 *
 * 按钮：
 * - BUTTON_SET_FILTER (0)：读取容器槽位的流体 → 写入过滤
 * - BUTTON_CLEAR_FILTER (1)：清除过滤
 * - BUTTON_CYCLE_DIRECTION (2)：循环工作方向
 *
 * PropertyDelegate：
 * - Index 0：方向序号（0~5=Direction 枚举，6=null/任意）
 * - Index 1：流体原始注册 ID（0=无过滤）
 */
@ModScreenHandler(name = "fluid_upgrade")
class FluidUpgradeScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val hand: Hand,
    private val containerInventory: Inventory = SimpleInventory(SLOT_CONTAINER_COUNT),
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(PROPERTY_COUNT)
) : ScreenHandler(FluidUpgradeScreenHandler::class.type(), syncId) {

    /** 方向序号：0~5 对应 Direction 枚举，6 表示 null/任意 */
    val directionOrdinal: Int get() = propertyDelegate.get(0)
    /** 流体的原始注册 ID（0=无过滤） */
    val fluidRawId: Int get() = propertyDelegate.get(1)

    companion object {
        const val SLOT_CONTAINER = 0
        const val SLOT_CONTAINER_COUNT = 1

        const val BUTTON_SET_FILTER = 0
        const val BUTTON_CLEAR_FILTER = 1
        const val BUTTON_CYCLE_DIRECTION = 2

        /** 方向序号 → null（任意方向） */
        private const val DIR_ORDINAL_NULL = 6

        private const val PROPERTY_COUNT = 2
        private const val PROP_DIRECTION = 0
        private const val PROP_FLUID = 1

        const val PLAYER_INV_START = SLOT_CONTAINER_COUNT
        private const val PLAYER_INV_END = PLAYER_INV_START + 27
        private const val HOTBAR_END = PLAYER_INV_END + 9

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): FluidUpgradeScreenHandler {
            val hand = buf.readEnumConstant(Hand::class.java)
            return FluidUpgradeScreenHandler(syncId, playerInventory, hand)
        }
    }

    init {
        addProperties(propertyDelegate)

        // 容器槽位
        addSlot(object : Slot(containerInventory, SLOT_CONTAINER, 0, 0) {
            override fun getMaxItemCount(): Int = 1
        })

        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 0, 0))
            }
        }
        // 快捷栏
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 0, 0))
        }

        refreshProperties()
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        val upgradeStack = player.getStackInHand(hand)
        if (upgradeStack.item !is FluidFilterUpgradeItem) return true

        when (id) {
            BUTTON_SET_FILTER -> {
                val container = containerInventory.getStack(SLOT_CONTAINER)
                val fluid = FluidPipeUpgradeComponent.readFluidFromItemStack(container)
                if (fluid == null) return true
                FluidPipeUpgradeComponent.writeFilter(upgradeStack, fluid)
            }
            BUTTON_CLEAR_FILTER -> {
                FluidPipeUpgradeComponent.writeFilter(upgradeStack, null)
            }
            BUTTON_CYCLE_DIRECTION -> {
                val current = FluidPipeUpgradeComponent.readDirection(upgradeStack)
                val next = FluidPipeUpgradeComponent.nextDirection(current)
                FluidPipeUpgradeComponent.writeDirection(upgradeStack, next)
            }
            else -> return false
        }

        refreshProperties()
        sendContentUpdates()
        return true
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        var moved = ItemStack.EMPTY
        val slot = slots[slotIndex]
        if (!slot.hasStack()) return ItemStack.EMPTY
        val stack = slot.stack
        moved = stack.copy()

        if (slotIndex == SLOT_CONTAINER) {
            // 容器槽 → 玩家背包
            if (!insertItem(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
        } else if (slotIndex in PLAYER_INV_START until HOTBAR_END) {
            // 玩家物品 → 容器槽（仅限可检测到流体的容器）
            if (FluidPipeUpgradeComponent.readFluidFromItemStack(stack) != null) {
                if (!insertItem(stack, SLOT_CONTAINER, SLOT_CONTAINER + 1, false)) return ItemStack.EMPTY
            } else {
                return ItemStack.EMPTY
            }
        } else {
            return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.stack = ItemStack.EMPTY
        else slot.markDirty()
        return moved
    }

    override fun canUse(player: PlayerEntity): Boolean {
        val stack = player.getStackInHand(hand)
        return stack.item is FluidFilterUpgradeItem
    }

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        dropInventory(player, containerInventory)
    }

    /** 从升级物品中读取当前状态并写入 PropertyDelegate。 */
    private fun refreshProperties() {
        val player = playerInventory.player
        val upgradeStack = player.getStackInHand(hand)
        if (upgradeStack.item !is FluidFilterUpgradeItem) {
            propertyDelegate.set(PROP_DIRECTION, DIR_ORDINAL_NULL)
            propertyDelegate.set(PROP_FLUID, 0)
            return
        }

        val dir = FluidPipeUpgradeComponent.readDirection(upgradeStack)
        propertyDelegate.set(PROP_DIRECTION, dir?.ordinal ?: DIR_ORDINAL_NULL)

        val filter = FluidPipeUpgradeComponent.readFilter(upgradeStack)
        propertyDelegate.set(PROP_FLUID, if (filter != null) Registries.FLUID.getRawId(filter) else 0)
    }
}

package ic2_120.content.screen

import ic2_120.content.item.ItemFilterUpgradeItem
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenHandlerMode
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Hand
import net.minecraft.util.math.Direction

/**
 * 物品升级配置 GUI。
 * 适用于 [ItemFilterUpgradeItem]（物品弹出升级和物品抽入升级共用）。
 *
 * 按钮：
 * - BUTTON_SET_FILTER (0)：读取容器槽位的物品 → 写入过滤
 * - BUTTON_CLEAR_FILTER (1)：清除过滤
 * - BUTTON_TOGGLE_DIR (10~15)：开关各方向
 *
 * PropertyDelegate：
 * - Index 0~5：6 个方向是否激活（0/1）
 * - Index 6：物品原始注册 ID（0=无过滤）
 */
@ModScreenHandler(name = "item_upgrade", mode = ScreenHandlerMode.HANDHELD)
class ItemUpgradeScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val hand: Hand,
    private val containerInventory: Inventory = SimpleInventory(SLOT_CONTAINER_COUNT),
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(PROPERTY_COUNT)
) : ScreenHandler(ItemUpgradeScreenHandler::class.type(), syncId) {

    fun isDirectionActive(dirIdx: Int): Boolean = propertyDelegate.get(PROP_DIR_BASE + dirIdx) != 0
    val itemRawId: Int get() = propertyDelegate.get(1)

    companion object {
        const val SLOT_CONTAINER = 0
        const val SLOT_CONTAINER_COUNT = 1

        const val BUTTON_SET_FILTER = 0
        const val BUTTON_CLEAR_FILTER = 1
        const val BUTTON_TOGGLE_DIR = 10

        private const val PROP_DIR_BASE = 0
        private const val PROP_ITEM = 6
        private const val PROPERTY_COUNT = 7

        const val PLAYER_INV_START = 1
        private const val PLAYER_INV_END = PLAYER_INV_START + 27
        private const val HOTBAR_END = PLAYER_INV_END + 9

    }

    init {
        addProperties(propertyDelegate)

        addSlot(object : Slot(containerInventory, SLOT_CONTAINER, 8, 35) {
            override fun getMaxItemCount(): Int = 1
        })

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }

        refreshProperties()
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        val upgradeStack = player.getStackInHand(hand)
        if (upgradeStack.item !is ItemFilterUpgradeItem) return true

        when (id) {
            BUTTON_SET_FILTER -> {
                val container = containerInventory.getStack(SLOT_CONTAINER)
                if (container.isEmpty) return true
                EjectorUpgradeComponent.writeFilter(upgradeStack, container.item)
            }
            BUTTON_CLEAR_FILTER -> {
                EjectorUpgradeComponent.writeFilter(upgradeStack, null)
            }
            in BUTTON_TOGGLE_DIR until BUTTON_TOGGLE_DIR + 6 -> {
                val dir = Direction.entries[id - BUTTON_TOGGLE_DIR]
                val current = EjectorUpgradeComponent.readDirections(upgradeStack)
                val next = if (dir in current) current - dir else current + dir
                EjectorUpgradeComponent.writeDirections(upgradeStack, next)
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
            if (!insertItem(stack, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
        } else if (slotIndex in PLAYER_INV_START until HOTBAR_END) {
            if (!insertItem(stack, SLOT_CONTAINER, SLOT_CONTAINER + 1, false)) return ItemStack.EMPTY
        } else {
            return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.stack = ItemStack.EMPTY
        else slot.markDirty()
        return moved
    }

    override fun canUse(player: PlayerEntity): Boolean {
        val stack = player.getStackInHand(hand)
        return stack.item is ItemFilterUpgradeItem
    }

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        dropInventory(player, containerInventory)
    }

    private fun refreshProperties() {
        val player = playerInventory.player
        val upgradeStack = player.getStackInHand(hand)
        if (upgradeStack.item !is ItemFilterUpgradeItem) {
            for (i in 0..5) propertyDelegate.set(PROP_DIR_BASE + i, 0)
            propertyDelegate.set(PROP_ITEM, 0)
            return
        }

        val dirs = EjectorUpgradeComponent.readDirections(upgradeStack)
        for (i in 0..5) {
            propertyDelegate.set(PROP_DIR_BASE + i, if (Direction.entries[i] in dirs) 1 else 0)
        }

        val filter = EjectorUpgradeComponent.readFilter(upgradeStack)
        propertyDelegate.set(PROP_ITEM, if (filter != null) Registries.ITEM.getRawId(filter) else 0)
    }
}

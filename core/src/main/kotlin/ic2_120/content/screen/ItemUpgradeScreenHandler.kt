package ic2_120.content.screen

import ic2_120.content.item.ItemFilterUpgradeItem
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.ScreenHandlerMode
import ic2_120.registry.type
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.InteractionHand
import net.minecraft.core.Direction

/**
 * 物品升级配置 GUI。
 * 适用于 [ItemFilterUpgradeItem]（物品弹出升级和物品抽入升级共用）。
 *
 * - Slot 0：幽灵过滤区，不保存真实物品
 *
 * 按钮：
 * - BUTTON_CLEAR_FILTER (1)：清除过滤
 * - BUTTON_TOGGLE_DIR (10~15)：开关各方向
 *
 * ContainerData：
 * - Index 0~5：6 个方向是否激活（0/1）
 * - Index 6：物品原始注册 ID（0=无过滤）
 */
@ModScreenHandler(name = "item_upgrade", mode = ScreenHandlerMode.HANDHELD)
class ItemUpgradeScreenHandler(
    syncId: Int,
    private val playerInventory: Inventory,
    private val hand: InteractionHand,
    private val filterInventory: SimpleContainer = SimpleContainer(1),
    private val propertyDelegate: ContainerData = SimpleContainerData(PROPERTY_COUNT)
) : AbstractContainerMenu(ItemUpgradeScreenHandler::class.type(), syncId) {

    fun isDirectionActive(dirIdx: Int): Boolean = propertyDelegate.get(PROP_DIR_BASE + dirIdx) != 0
    val itemRawId: Int get() = propertyDelegate.getValue(PROP_ITEM)
    val filterSlotIndex: Int get() = FILTER_SLOT

    companion object {
        const val FILTER_SLOT = 0

        const val BUTTON_CLEAR_FILTER = 1
        const val BUTTON_TOGGLE_DIR = 10

        private const val PROP_DIR_BASE = 0
        private const val PROP_ITEM = 6
        private const val PROPERTY_COUNT = 7

        const val PLAYER_INV_START = 1

    }

    init {
        addDataSlots(propertyDelegate)

        addSlot(object : Slot(filterInventory, FILTER_SLOT, 8, 35) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: Player): Boolean = false
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

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        val upgradeStack = player.getItemInHand(hand)
        if (upgradeStack.item !is ItemFilterUpgradeItem) return true

        when (id) {
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

    /** AE2-style fake-slot setter used by both JEI and the local fallback. */
    fun setItemFilter(slotIndex: Int, stack: ItemStack): Boolean {
        if (slotIndex != FILTER_SLOT) return false
        val upgradeStack = playerInventory.player.getItemInHand(hand)
        if (upgradeStack.item !is ItemFilterUpgradeItem) return false
        val ghostStack = if (stack.isEmpty) ItemStack.EMPTY else stack.copy().also { it.count = 1 }
        filterInventory.setStack(FILTER_SLOT, ghostStack)
        EjectorUpgradeComponent.writeFilter(upgradeStack, ghostStack.item.takeUnless { ghostStack.isEmpty })
        playerInventory.setChanged()
        refreshProperties()
        sendContentUpdates()
        return true
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: ClickType, player: Player) {
        if (slotIndex == FILTER_SLOT && actionType == ClickType.PICKUP) {
            val cursor = cursorStack
            if (cursor.isEmpty || button == 1) {
                setItemFilter(FILTER_SLOT, ItemStack.EMPTY)
            } else if (setItemFilter(FILTER_SLOT, cursor)) {
                returnCursorToInventory()
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        val stack = player.getItemInHand(hand)
        return stack.item is ItemFilterUpgradeItem
    }

    private fun returnCursorToInventory() {
        val returning = cursorStack.copy()
        if (returning.isEmpty) return
        playerInventory.safeInsert(returning)
        cursorStack = if (returning.isEmpty) ItemStack.EMPTY else returning
    }

    private fun refreshProperties() {
        val player = playerInventory.player
        val upgradeStack = player.getItemInHand(hand)
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
        filterInventory.setStack(FILTER_SLOT, filter?.defaultStack ?: ItemStack.EMPTY)
        propertyDelegate.set(PROP_ITEM, if (filter != null) BuiltInRegistries.ITEM.getRawId(filter) else 0)
    }
}

package ic2_120.content.screen

import ic2_120.content.item.ItemFilterUpgradeItem
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.registry.annotation.ModScreenHandler
<<<<<<< HEAD
import ic2_120.registry.annotation.ScreenHandlerMode
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
=======
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
>>>>>>> backup/1.21.1
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
<<<<<<< HEAD
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import net.minecraft.util.math.Direction
=======
import net.minecraft.util.Hand
>>>>>>> backup/1.21.1

/**
 * 物品升级配置 GUI。
 * 适用于 [ItemFilterUpgradeItem]（物品弹出升级和物品抽入升级共用）。
 *
<<<<<<< HEAD
 * - Slot 0：幽灵过滤区，不保存真实物品
 *
 * 按钮：
 * - BUTTON_CLEAR_FILTER (1)：清除过滤
 * - BUTTON_TOGGLE_DIR (10~15)：开关各方向
 *
 * PropertyDelegate：
 * - Index 0~5：6 个方向是否激活（0/1）
 * - Index 6：物品原始注册 ID（0=无过滤）
 */
@ModScreenHandler(name = "item_upgrade", mode = ScreenHandlerMode.HANDHELD)
=======
 * 按钮：
 * - BUTTON_SET_FILTER (0)：读取容器槽位的物品 → 写入过滤
 * - BUTTON_CLEAR_FILTER (1)：清除过滤
 * - BUTTON_CYCLE_DIRECTION (2)：循环工作方向
 *
 * PropertyDelegate：
 * - Index 0：方向序号（0~5=Direction 枚举，6=null/任意）
 * - Index 1：物品原始注册 ID（0=无过滤）
 */
@ModScreenHandler(name = "item_upgrade")
>>>>>>> backup/1.21.1
class ItemUpgradeScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val hand: Hand,
<<<<<<< HEAD
    private val filterInventory: SimpleInventory = SimpleInventory(1),
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(PROPERTY_COUNT)
) : ScreenHandler(ItemUpgradeScreenHandler::class.type(), syncId) {

    fun isDirectionActive(dirIdx: Int): Boolean = propertyDelegate.get(PROP_DIR_BASE + dirIdx) != 0
    val itemRawId: Int get() = propertyDelegate.get(PROP_ITEM)
    val filterSlotIndex: Int get() = FILTER_SLOT

    companion object {
        const val FILTER_SLOT = 0

        const val BUTTON_CLEAR_FILTER = 1
        const val BUTTON_TOGGLE_DIR = 10

        private const val PROP_DIR_BASE = 0
        private const val PROP_ITEM = 6
        private const val PROPERTY_COUNT = 7

        const val PLAYER_INV_START = 1

=======
    private val containerInventory: Inventory = SimpleInventory(SLOT_CONTAINER_COUNT),
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(PROPERTY_COUNT)
) : ScreenHandler(ItemUpgradeScreenHandler::class.type(), syncId) {

    val directionOrdinal: Int get() = propertyDelegate.get(0)
    val itemRawId: Int get() = propertyDelegate.get(1)

    companion object {
        const val SLOT_CONTAINER = 0
        const val SLOT_CONTAINER_COUNT = 1

        const val BUTTON_SET_FILTER = 0
        const val BUTTON_CLEAR_FILTER = 1
        const val BUTTON_CYCLE_DIRECTION = 2

        private const val DIR_ORDINAL_NULL = 6
        private const val PROPERTY_COUNT = 2
        private const val PROP_DIRECTION = 0
        private const val PROP_ITEM = 1

        const val PLAYER_INV_START = 1
        private const val PLAYER_INV_END = PLAYER_INV_START + 27
        private const val HOTBAR_END = PLAYER_INV_END + 9

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ItemUpgradeScreenHandler {
            val hand = buf.readEnumConstant(Hand::class.java)
            return ItemUpgradeScreenHandler(syncId, playerInventory, hand)
        }
>>>>>>> backup/1.21.1
    }

    init {
        addProperties(propertyDelegate)

<<<<<<< HEAD
        addSlot(object : Slot(filterInventory, FILTER_SLOT, 8, 35) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
=======
        addSlot(object : Slot(containerInventory, SLOT_CONTAINER, 8, 35) {
            override fun getMaxItemCount(): Int = 1
>>>>>>> backup/1.21.1
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
<<<<<<< HEAD
            BUTTON_CLEAR_FILTER -> {
                EjectorUpgradeComponent.writeFilter(upgradeStack, null)
            }
            in BUTTON_TOGGLE_DIR until BUTTON_TOGGLE_DIR + 6 -> {
                val dir = Direction.entries[id - BUTTON_TOGGLE_DIR]
                val current = EjectorUpgradeComponent.readDirections(upgradeStack)
                val next = if (dir in current) current - dir else current + dir
                EjectorUpgradeComponent.writeDirections(upgradeStack, next)
=======
            BUTTON_SET_FILTER -> {
                val container = containerInventory.getStack(SLOT_CONTAINER)
                if (container.isEmpty) return true
                EjectorUpgradeComponent.writeFilter(upgradeStack, container.item)
            }
            BUTTON_CLEAR_FILTER -> {
                EjectorUpgradeComponent.writeFilter(upgradeStack, null)
            }
            BUTTON_CYCLE_DIRECTION -> {
                val current = EjectorUpgradeComponent.readDirection(upgradeStack)
                val next = EjectorUpgradeComponent.nextDirection(current)
                EjectorUpgradeComponent.writeDirection(upgradeStack, next)
>>>>>>> backup/1.21.1
            }
            else -> return false
        }

        refreshProperties()
        sendContentUpdates()
        return true
    }

<<<<<<< HEAD
    /** AE2-style fake-slot setter used by both JEI and the local fallback. */
    fun setItemFilter(slotIndex: Int, stack: ItemStack): Boolean {
        if (slotIndex != FILTER_SLOT) return false
        val upgradeStack = playerInventory.player.getStackInHand(hand)
        if (upgradeStack.item !is ItemFilterUpgradeItem) return false
        val ghostStack = if (stack.isEmpty) ItemStack.EMPTY else stack.copy().also { it.count = 1 }
        filterInventory.setStack(FILTER_SLOT, ghostStack)
        EjectorUpgradeComponent.writeFilter(upgradeStack, ghostStack.item.takeUnless { ghostStack.isEmpty })
        playerInventory.markDirty()
        refreshProperties()
        sendContentUpdates()
        return true
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex == FILTER_SLOT && actionType == SlotActionType.PICKUP) {
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

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        return ItemStack.EMPTY
=======
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
>>>>>>> backup/1.21.1
    }

    override fun canUse(player: PlayerEntity): Boolean {
        val stack = player.getStackInHand(hand)
        return stack.item is ItemFilterUpgradeItem
    }

<<<<<<< HEAD
    private fun returnCursorToInventory() {
        val returning = cursorStack.copy()
        if (returning.isEmpty) return
        playerInventory.insertStack(returning)
        cursorStack = if (returning.isEmpty) ItemStack.EMPTY else returning
=======
    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        dropInventory(player, containerInventory)
>>>>>>> backup/1.21.1
    }

    private fun refreshProperties() {
        val player = playerInventory.player
        val upgradeStack = player.getStackInHand(hand)
        if (upgradeStack.item !is ItemFilterUpgradeItem) {
<<<<<<< HEAD
            for (i in 0..5) propertyDelegate.set(PROP_DIR_BASE + i, 0)
=======
            propertyDelegate.set(PROP_DIRECTION, DIR_ORDINAL_NULL)
>>>>>>> backup/1.21.1
            propertyDelegate.set(PROP_ITEM, 0)
            return
        }

<<<<<<< HEAD
        val dirs = EjectorUpgradeComponent.readDirections(upgradeStack)
        for (i in 0..5) {
            propertyDelegate.set(PROP_DIR_BASE + i, if (Direction.entries[i] in dirs) 1 else 0)
        }

        val filter = EjectorUpgradeComponent.readFilter(upgradeStack)
        filterInventory.setStack(FILTER_SLOT, filter?.defaultStack ?: ItemStack.EMPTY)
=======
        val dir = EjectorUpgradeComponent.readDirection(upgradeStack)
        propertyDelegate.set(PROP_DIRECTION, dir?.ordinal ?: DIR_ORDINAL_NULL)

        val filter = EjectorUpgradeComponent.readFilter(upgradeStack)
>>>>>>> backup/1.21.1
        propertyDelegate.set(PROP_ITEM, if (filter != null) Registries.ITEM.getRawId(filter) else 0)
    }
}

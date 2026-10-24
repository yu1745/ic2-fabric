package ic2_120.content.screen

import ic2_120.content.item.FluidFilterUpgradeItem
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.registry.annotation.ModScreenHandler
<<<<<<< HEAD
import ic2_120.registry.annotation.ScreenHandlerMode
import ic2_120.registry.type
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.SimpleInventory
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
=======
>>>>>>> backup/1.21.1
import net.minecraft.util.Hand
import net.minecraft.util.math.Direction

/**
 * 流体升级配置 GUI。
 * 适用于 [FluidFilterUpgradeItem]（流体弹出升级和流体抽入升级共用）。
 *
 * 布局：
<<<<<<< HEAD
 * - Slot 0：幽灵过滤区（用光标上的桶/单元选择流体，不存放物品）
 * - 玩家背包及快捷栏
 *
 * 按钮：
=======
 * - Slot 0：容器槽位（玩家放入桶/单元以检测流体）
 * - 玩家背包及快捷栏
 *
 * 按钮：
 * - BUTTON_SET_FILTER (0)：读取容器槽位的流体 → 写入过滤
>>>>>>> backup/1.21.1
 * - BUTTON_CLEAR_FILTER (1)：清除过滤
 * - BUTTON_TOGGLE_DIR (10~15)：开关各方向
 *
 * PropertyDelegate：
 * - Index 0~5：6 个方向是否激活（0/1）
 * - Index 6：流体原始注册 ID（0=无过滤）
 */
<<<<<<< HEAD
@ModScreenHandler(name = "fluid_upgrade", mode = ScreenHandlerMode.HANDHELD)
=======
@ModScreenHandler(name = "fluid_upgrade")
>>>>>>> backup/1.21.1
class FluidUpgradeScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val hand: Hand,
<<<<<<< HEAD
=======
    private val containerInventory: Inventory = SimpleInventory(SLOT_CONTAINER_COUNT),
>>>>>>> backup/1.21.1
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(PROPERTY_COUNT)
) : ScreenHandler(FluidUpgradeScreenHandler::class.type(), syncId) {

    /** 方向是否激活 */
    fun isDirectionActive(dirIdx: Int): Boolean = propertyDelegate.get(PROP_DIR_BASE + dirIdx) != 0
    /** 当前切换方向索引 */
    val currentDirectionIndex: Int get() = propertyDelegate.get(PROP_DIR_INDEX)
    /** 流体的原始注册 ID（0=无过滤） */
    val fluidRawId: Int get() = propertyDelegate.get(PROP_FLUID)

    companion object {
<<<<<<< HEAD
        const val FILTER_SLOT = 0
        private const val FILTER_SLOT_COUNT = 1

=======
        const val SLOT_CONTAINER = 0
        const val SLOT_CONTAINER_COUNT = 1

        const val BUTTON_SET_FILTER = 0
>>>>>>> backup/1.21.1
        const val BUTTON_CLEAR_FILTER = 1
        const val BUTTON_CYCLE_DIRECTION = 8
        const val BUTTON_TOGGLE_DIR = 10  // 10-15

<<<<<<< HEAD
        private const val PROP_DIR_BASE = 0
        private const val PROP_FLUID = 6
        @Suppress("unused") private const val PROP_DIR_INDEX = 7
        private const val PROPERTY_COUNT = 8

        const val PLAYER_INV_START = FILTER_SLOT_COUNT
=======
        private const val PROP_DIR_BASE = 0    // 0-5: 6 directions
        private const val PROP_FLUID = 6
        private const val PROP_DIR_INDEX = 7   // 当前切换方向索引
        private const val PROPERTY_COUNT = 8

        const val PLAYER_INV_START = SLOT_CONTAINER_COUNT
        private const val PLAYER_INV_END = PLAYER_INV_START + 27
        private const val HOTBAR_END = PLAYER_INV_END + 9

        @ScreenFactory
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): FluidUpgradeScreenHandler {
            val hand = buf.readEnumConstant(Hand::class.java)
            return FluidUpgradeScreenHandler(syncId, playerInventory, hand)
        }
>>>>>>> backup/1.21.1
    }

    init {
        addProperties(propertyDelegate)

<<<<<<< HEAD
        // 幽灵过滤区始终为空，只接受 onSlotClick 中的流体样本选择。
        addSlot(object : Slot(SimpleInventory(FILTER_SLOT_COUNT), FILTER_SLOT, 8, 35) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
=======
        // 容器槽位
        addSlot(object : Slot(containerInventory, SLOT_CONTAINER, 8, 35) {
            override fun getMaxItemCount(): Int = 1
>>>>>>> backup/1.21.1
        })

        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        // 快捷栏
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }

        refreshProperties()
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        val upgradeStack = player.getStackInHand(hand)
        if (upgradeStack.item !is FluidFilterUpgradeItem) return true

        when (id) {
<<<<<<< HEAD
=======
            BUTTON_SET_FILTER -> {
                val container = containerInventory.getStack(SLOT_CONTAINER)
                val fluid = FluidPipeUpgradeComponent.readFluidFromItemStack(container)
                if (fluid == null) return true
                FluidPipeUpgradeComponent.writeFilter(upgradeStack, fluid)
            }
>>>>>>> backup/1.21.1
            BUTTON_CLEAR_FILTER -> {
                FluidPipeUpgradeComponent.writeFilter(upgradeStack, null)
            }
            BUTTON_CYCLE_DIRECTION -> {
<<<<<<< HEAD
                val current = FluidPipeUpgradeComponent.readDirections(upgradeStack)
                val next = FluidPipeUpgradeComponent.nextDirections(current)
                FluidPipeUpgradeComponent.writeDirections(upgradeStack, next)
=======
                val idx = propertyDelegate.get(PROP_DIR_INDEX)
                propertyDelegate.set(PROP_DIR_INDEX, (idx + 1) % 6)
>>>>>>> backup/1.21.1
            }
            in BUTTON_TOGGLE_DIR until BUTTON_TOGGLE_DIR + 6 -> {
                val dirIdx = id - BUTTON_TOGGLE_DIR
                val dir = Direction.entries[dirIdx]
                val current = FluidPipeUpgradeComponent.readDirections(upgradeStack)
                val next = if (dir in current) current - setOf(dir) else current + setOf(dir)
                FluidPipeUpgradeComponent.writeDirections(upgradeStack, next)
            }
            else -> return false
        }

        refreshProperties()
        sendContentUpdates()
        return true
    }

<<<<<<< HEAD
    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex == FILTER_SLOT && actionType == SlotActionType.PICKUP) {
            val cursor = cursorStack
            if (cursor.isEmpty || button == 1) {
                clearFluidFilter()
            } else {
                val fluid = FluidPipeUpgradeComponent.readFluidFromItemStack(cursor)
                if (fluid != null && setFluidFilter(fluid)) {
                    returnCursorToInventory()
                }
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    /** 设置幽灵流体过滤器；JEI 拖放和真实容器取样共用。 */
    fun setFluidFilter(fluid: Fluid): Boolean {
        val upgradeStack = playerInventory.player.getStackInHand(hand)
        if (upgradeStack.item !is FluidFilterUpgradeItem) return false
        FluidPipeUpgradeComponent.writeFilter(upgradeStack, fluid)
        refreshProperties()
        sendContentUpdates()
        return true
    }

    private fun clearFluidFilter(): Boolean {
        val upgradeStack = playerInventory.player.getStackInHand(hand)
        if (upgradeStack.item !is FluidFilterUpgradeItem) return false
        FluidPipeUpgradeComponent.writeFilter(upgradeStack, null)
        refreshProperties()
        sendContentUpdates()
        return true
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

=======
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

>>>>>>> backup/1.21.1
    override fun canUse(player: PlayerEntity): Boolean {
        val stack = player.getStackInHand(hand)
        return stack.item is FluidFilterUpgradeItem
    }

<<<<<<< HEAD
    /** 真实容器仅作为过滤样本；设置后放回背包，不占用过滤槽。 */
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

    /** 从升级物品中读取当前状态并写入 PropertyDelegate。 */
    private fun refreshProperties() {
        val player = playerInventory.player
        val upgradeStack = player.getStackInHand(hand)
        if (upgradeStack.item !is FluidFilterUpgradeItem) {
            for (i in 0..5) propertyDelegate.set(PROP_DIR_BASE + i, 0)
            propertyDelegate.set(PROP_FLUID, 0)
            return
        }

        val dirs = FluidPipeUpgradeComponent.readDirections(upgradeStack)
        for (i in 0..5) {
            propertyDelegate.set(PROP_DIR_BASE + i, if (Direction.entries[i] in dirs) 1 else 0)
        }

        val filter = FluidPipeUpgradeComponent.readFilter(upgradeStack)
        propertyDelegate.set(PROP_FLUID, if (filter != null) Registries.FLUID.getRawId(filter) else 0)
    }
}

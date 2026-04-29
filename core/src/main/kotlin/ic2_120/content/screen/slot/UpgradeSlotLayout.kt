package ic2_120.content.screen.slot

import ic2_120.content.item.IUpgradeItem
import ic2_120.content.upgrade.UpgradeItemRegistry
import net.minecraft.item.ItemStack

/**
 * 可复用的升级槽位布局与规则。
 *
 * 每个机器有 4 个升级槽位，紧贴原版 UI（176 宽）右侧纵向排列。
 * 槽位仅允许放入 [IUpgradeItem] 且在 [UpgradeItemRegistry] 中注册、且机器实现了对应接口的升级。
 */
object UpgradeSlotLayout {

    /** 升级槽数量 */
    const val SLOT_COUNT = 4

    /** 原版 UI 宽度，升级槽紧贴其右侧 */
    const val VANILLA_UI_WIDTH = 176

    /** 升级槽列 X 坐标（紧贴 176 右侧） */
    const val SLOT_X = VANILLA_UI_WIDTH

    /** 第一个升级槽 Y 坐标 */
    const val SLOT_Y_FIRST = 17

    /** 槽位间距 */
    const val SLOT_SPACING = 18

    /**
     * 创建升级槽位规则，需传入机器获取器（如从 ScreenHandlerContext 获取 BlockEntity）。
     * 仅当机器实现了该升级对应的接口时才允许放入。
     */
    fun slotSpec(machineProvider: () -> Any?): SlotSpec = SlotSpec(
        canInsert = { stack ->
            if (stack.isEmpty || stack.item !is IUpgradeItem) return@SlotSpec false
            UpgradeItemRegistry.canAccept(machineProvider(), stack.item)
        }
    )

    /**
     * 获取第 [index] 个升级槽的 Y 坐标（0..3）。
     */
    fun slotY(index: Int): Int = SLOT_Y_FIRST + index * SLOT_SPACING
}

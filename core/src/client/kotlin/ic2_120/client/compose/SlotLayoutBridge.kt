package ic2_120.client.compose

import net.minecraft.screen.ScreenHandler

/**
 * 将 Compose 布局导出的锚点转换为 Slot 相对 GUI 的坐标。
 */
object SlotLayoutBridge {
    /**
     * @param anchors Compose 布局快照中的锚点矩形（屏幕绝对坐标）
     * @param handler 当前 screen handler
     * @param screenLeft HandledScreen.x（GUI 左上角）
     * @param screenTop HandledScreen.y（GUI 左上角）
     * @param mapping anchorId -> slotIndex
     */
    fun apply(
        anchors: Map<String, RenderContext.AnchorRect>,
        handler: ScreenHandler,
        screenLeft: Int,
        screenTop: Int,
        mapping: Map<String, Int>
    ) {
        for ((anchorId, slotIndex) in mapping) {
            val anchor = anchors[anchorId] ?: continue
            val slot = handler.slots.getOrNull(slotIndex) ?: continue
            val relativeX = anchor.x - screenLeft
            val relativeY = anchor.y - screenTop
            slot.x = relativeX
            slot.y = relativeY
        }
    }
}

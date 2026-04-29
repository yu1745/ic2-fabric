package ic2_120.client.compose

/**
 * 标准 3×9 主背包 + 快捷栏的 [SlotAnchor] 布局。
 *
 * @param slotColumnLeftOffset 第一列槽位相对 GUI 左缘的 X 偏移（标准面板为 8；核电等为 [ic2_120.content.screen.NuclearReactorScreenHandler.PLAYER_INV_X]）
 */
fun UiScope.playerInventoryAndHotbarSlotAnchors(
    left: Int,
    top: Int,
    playerInvStart: Int,
    playerInvY: Int,
    hotbarY: Int,
    slotSize: Int = 18,
    slotColumnLeftOffset: Int = 8,
    slotAnchorId: (Int) -> String = { "slot.$it" }
) {
    val baseX = left + slotColumnLeftOffset
    Flex(x = baseX, y = top + playerInvY) {
        Column(spacing = 0) {
            for (row in 0 until 3) {
                Row(spacing = 0) {
                    for (col in 0 until 9) {
                        val slotIndex = playerInvStart + col + row * 9
                        SlotAnchor(
                            id = slotAnchorId(slotIndex),
                            width = slotSize,
                            height = slotSize,
                            showBorder = false
                        )
                    }
                }
            }
        }
    }
    Flex(x = baseX, y = top + hotbarY) {
        Row(spacing = 0) {
            for (col in 0 until 9) {
                val slotIndex = playerInvStart + 27 + col
                SlotAnchor(
                    id = slotAnchorId(slotIndex),
                    width = slotSize,
                    height = slotSize,
                    showBorder = false
                )
            }
        }
    }
}

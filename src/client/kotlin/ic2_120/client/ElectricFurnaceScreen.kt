package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.screen.ElectricFurnaceScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = ElectricFurnaceBlock::class)
class ElectricFurnaceScreen(
    handler: ElectricFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ElectricFurnaceScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - backgroundWidth) / 2
        val y = (height - backgroundHeight) / 2
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = (width - backgroundWidth) / 2
        val top = (height - backgroundHeight) / 2

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top - 16, spacing = 2) {
                Text("IC2 电炉", color = 0xFFFFFF)
                Text("同步计数: ${handler.sync.syncCounter}", color = 0xAAAAAA, shadow = false)
                Text("能量: ${handler.sync.energy}", color = 0xAAAAAA, shadow = false)
                Text("进度: ${handler.sync.progress}", color = 0xAAAAAA, shadow = false)
            }

            Row(x = left + 8, y = top + 8, spacing = 4) {
                Column(
                    spacing = 2,
                    modifier = Modifier.EMPTY
                        .background(0x80FF0000.toInt())
                        .padding(4)
                ) {
                    Text("Input", color = 0xFFFFFF)
                    Text("Slot 0", color = 0xCCCCCC)
                }
                Column(
                    spacing = 2,
                    modifier = Modifier.EMPTY
                        .background(0x8000FF00.toInt())
                        .padding(4)
                ) {
                    Text("Output", color = 0xFFFFFF)
                    Text("Slot 1", color = 0xCCCCCC)
                }
            }

            Flex(
                x = left, y = top + backgroundHeight + 2,
                direction = FlexDirection.ROW,
                justifyContent = JustifyContent.SPACE_BETWEEN,
                alignItems = AlignItems.CENTER,
                gap = 4,
                modifier = Modifier.EMPTY.width(backgroundWidth).height(20)
            ) {
                Button(
                    "启动",
                    modifier = Modifier.EMPTY.background(0xFF006600.toInt())
                ) {
                    println("[IC2] 启动按钮被点击")
                }
                Text("Electric Furnace", color = 0x999999)
                Button(
                    "停止",
                    modifier = Modifier.EMPTY.background(0xFF660000.toInt())
                ) {
                    println("[IC2] 停止按钮被点击")
                }
            }

            Column(x = left + backgroundWidth + 4, y = top, spacing = 2) {
                Image(
                    Identifier("minecraft", "textures/block/diamond_block.png"),
                    width = 16, height = 16
                )
                Image(
                    Identifier("minecraft", "textures/block/gold_block.png"),
                    width = 16, height = 16
                )
            }

            Text(
                "绝对定位测试", x = left + backgroundWidth - 60, y = top - 10, absolute = true,
                color = 0xFFFF00
            )

            // 能量条测试：Table 对齐 0 / 25 / 50 / 75 / 100
            Column(x = left + 8, y = top + 60, spacing = 4) {
                Text("能量条", color = 0xFFFFFF)
                Table(columnWidths = listOf(28, 100), columnSpacing = 8, rowSpacing = 4) {
                    row { Text("0%", color = 0xAAAAAA); EnergyBar(0f) }
                    row { Text("25%", color = 0xAAAAAA); EnergyBar(0.25f) }
                    row { Text("50%", color = 0xAAAAAA); EnergyBar(0.5f) }
                    row { Text("75%", color = 0xAAAAAA); EnergyBar(0.75f) }
                    row { Text("100%", color = 0xAAAAAA); EnergyBar(1f) }
                }
            }
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val TEXTURE = Identifier("minecraft", "textures/gui/container/furnace.png")
    }
}

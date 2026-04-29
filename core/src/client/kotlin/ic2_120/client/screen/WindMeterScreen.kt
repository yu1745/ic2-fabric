package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.GuiBackground
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.WindMeterScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(handler = "wind_meter")
class WindMeterScreen(
    handler: WindMeterScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<WindMeterScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = -1000
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 4,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Text(title.string, color = 0xFFFFFF)
                Text(t("gui.ic2_120.wind_meter.mean_wind", permilleToPct(handler.getValue(WindMeterScreenHandler.IDX_MEAN_PERMILLE))), color = 0xAAAAAA, shadow = false)
                Text(t("gui.ic2_120.wind_meter.weather_bonus", permilleToMultiplier(handler.getValue(WindMeterScreenHandler.IDX_WEATHER_PERMILLE))), color = 0xAAAAAA, shadow = false)
                Text(t("gui.ic2_120.wind_meter.gust_bonus", permilleToMultiplier(handler.getValue(WindMeterScreenHandler.IDX_GUST_PERMILLE))), color = 0xAAAAAA, shadow = false)
                Text(t("gui.ic2_120.wind_meter.effective_wind", permilleToPct(handler.getValue(WindMeterScreenHandler.IDX_EFFECTIVE_PERMILLE))), color = 0xAAAAAA, shadow = false)
                Text(rotorLine(t("gui.ic2_120.wind_meter.wood"), handler.getValue(WindMeterScreenHandler.IDX_WOOD_KU), handler.getValue(WindMeterScreenHandler.IDX_WOOD_START_Y)), color = 0xAAAAAA, shadow = false)
                Text(rotorLine(t("gui.ic2_120.wind_meter.iron"), handler.getValue(WindMeterScreenHandler.IDX_IRON_KU), handler.getValue(WindMeterScreenHandler.IDX_IRON_START_Y)), color = 0xAAAAAA, shadow = false)
                Text(rotorLine(t("gui.ic2_120.wind_meter.steel"), handler.getValue(WindMeterScreenHandler.IDX_STEEL_KU), handler.getValue(WindMeterScreenHandler.IDX_STEEL_START_Y)), color = 0xAAAAAA, shadow = false)
                Text(rotorLine(t("gui.ic2_120.wind_meter.carbon"), handler.getValue(WindMeterScreenHandler.IDX_CARBON_KU), handler.getValue(WindMeterScreenHandler.IDX_CARBON_START_Y)), color = 0xAAAAAA, shadow = false)
            }

        }
        ui.layout(context, textRenderer, mouseX, mouseY, content = content)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun permilleToPct(v: Int): String = String.format("%.1f%%", v / 10.0)

    private fun permilleToMultiplier(v: Int): String = String.format("%.2fx", v / 1000.0)

    private fun rotorLine(name: String, ku: Int, requiredY: Int): String {
        if (requiredY >= 0) {
            return t("gui.ic2_120.wind_meter.rotor_need_height", name, ku, requiredY)
        }
        if (requiredY == -1) {
            return t("gui.ic2_120.wind_meter.rotor_no_weather", name, ku)
        }
        return t("gui.ic2_120.wind_meter.rotor_output", name, ku)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}

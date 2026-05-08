package ic2_120.integration.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

class ModMenuIntegration : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            try {
                Ic2ConfigScreen.createScreen(parent)
            } catch (e: NoClassDefFoundError) {
                LOGGER.error("Cloth Config not found", e)
                errorScreen(parent, "需要安装 Cloth Config API 才能编辑配置")
            } catch (e: Exception) {
                LOGGER.error("Failed to create IC2 config screen", e)
                errorScreen(parent, "配置界面加载失败: ${e.message}")
            }
        }
    }

    private fun errorScreen(parent: Screen?, message: String): Screen {
        val text = Text.literal("IC2 Config")
        return object : Screen(text) {
            override fun init() {
                addDrawableChild(
                    net.minecraft.client.gui.widget.ButtonWidget.builder(
                        Text.literal("§c$message"),
                        { client!!.setScreen(parent) }
                    ).dimensions(width / 2 - 100, height / 2 - 10, 200, 20).build()
                )
                addDrawableChild(
                    net.minecraft.client.gui.widget.ButtonWidget.builder(
                        Text.literal("返回"),
                        { client!!.setScreen(parent) }
                    ).dimensions(width / 2 - 50, height / 2 + 20, 100, 20).build()
                )
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger("IC2-ModMenu")
    }
}

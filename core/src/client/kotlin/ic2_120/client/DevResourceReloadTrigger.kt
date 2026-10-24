package ic2_120.client

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

/**
 * 开发环境资源重载触发器：游戏目录下出现 `.ic2_reload_resources` 文件时删除它并重载资源（等同 F3+T）。
 *
 * 供 tools/model3d 的截图闭环使用：桌面上的截图工具（如 Snipaste）会全局抢占 F1/F3，模拟按键不可靠。
 * 仅开发环境注册。
 */
object DevResourceReloadTrigger {

    private const val TRIGGER = ".ic2_reload_resources"

    fun register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
        val file = FabricLoader.getInstance().gameDir.resolve(TRIGGER)
        var cooldown = 0
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (--cooldown > 0) return@register
            cooldown = 10
            if (Files.deleteIfExists(file)) client.reloadResources()
        }
    }
}

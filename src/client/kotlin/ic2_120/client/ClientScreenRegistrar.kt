package ic2_120.client

import ic2_120.registry.annotation.ModScreen
import net.minecraft.block.Block
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

/**
 * 客户端 Screen 自动注册。
 * 扫描指定包中带 [ModScreen] 注解的 [HandledScreen] 子类，并注册到 [HandledScreens]。
 */
object ClientScreenRegistrar {

    private val logger = LoggerFactory.getLogger("ic2_120/ClientScreenRegistrar")

    /**
     * 扫描并注册所有带 @ModScreen 的 Screen 类。
     *
     * @param modId 模组命名空间
     * @param packageNames 要扫描的包名列表（通常为 client 包）
     */
    fun registerScreens(modId: String, packageNames: List<String>) {
        logger.info("开始扫描客户端包以注册 Screen: {}", packageNames)
        for (packageName in packageNames) {
            scanAndRegisterPackage(modId, packageName)
        }
        logger.info("客户端 Screen 注册完成")
    }

    private fun scanAndRegisterPackage(modId: String, packageName: String) {
        try {
            val classLoader = Thread.currentThread().contextClassLoader
            val packagePath = packageName.replace('.', '/')
            val resources = classLoader.getResources(packagePath)
            while (resources.hasMoreElements()) {
                val url = resources.nextElement()
                if (url.protocol == "file") {
                    scanDirectory(url.path, packageName, modId)
                }
            }
        } catch (e: Exception) {
            logger.error("扫描包 {} 失败: {}", packageName, e.message, e)
        }
    }

    private fun scanDirectory(path: String, packageName: String, modId: String) {
        val dir = java.io.File(path)
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanDirectory(file.path, "$packageName.${file.name}", modId)
                file.name.endsWith(".class") -> {
                    val className = "$packageName.${file.name.removeSuffix(".class")}"
                    processClass(className, modId)
                }
            }
        }
    }

    private fun processClass(className: String, modId: String) {
        try {
            val clazz = Class.forName(className).kotlin
            val modScreen = clazz.findAnnotation<ModScreen>() ?: return
            if (!clazz.isSubclassOf(HandledScreen::class)) {
                logger.warn("@ModScreen 类 {} 不是 HandledScreen 的子类", className)
                return
            }
            val handlerName = resolveHandlerName(modScreen, modId)
                ?: run {
                    logger.error("@ModScreen 类 {} 未指定 handler 或 block，或根据 block 未找到对应方块注册名", className)
                    return
                }
            val id = Identifier(modId, handlerName)
            val type = Registries.SCREEN_HANDLER.get(id)
                ?: run {
                    logger.error("未找到 ScreenHandler 类型: {}，请确保主端已注册 @ModScreenHandler(name = \"{}\")", id, handlerName)
                    return
                }
            val ctor = clazz.primaryConstructor
                ?: error("@ModScreen 类 ${clazz.simpleName} 需有主构造函数 (handler, playerInventory, title)")
            @Suppress("UNCHECKED_CAST")
            val screenType = type as ScreenHandlerType<ScreenHandler>
            HandledScreens.register(screenType) { handler, playerInventory, title ->
                ctor.call(handler, playerInventory, title) as HandledScreen<ScreenHandler>
            }
            logger.info("已注册客户端 Screen: {} -> {}", clazz.simpleName, id)
        } catch (e: ClassNotFoundException) {
            logger.warn("无法加载类: {}", className)
        } catch (e: Exception) {
            logger.debug("处理类 {} 时出错: {}", className, e.message)
        }
    }

    /** 从 @ModScreen 的 handler 或 block 解析出 ScreenHandler 注册名（path）。 */
    private fun resolveHandlerName(annotation: ModScreen, modId: String): String? {
        if (annotation.handler.isNotEmpty()) return annotation.handler
        if (annotation.block == Any::class) return null
        if (!annotation.block.isSubclassOf(Block::class)) return null
        for (id in Registries.BLOCK.ids) {
            if (id.namespace != modId) continue
            val block = Registries.BLOCK.get(id)
            if (block::class == annotation.block) return id.path
        }
        return null
    }
}

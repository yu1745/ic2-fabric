package ic2_120.content.screen

import ic2_120.registry.ScreenHandlerTypeStore
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import kotlin.reflect.KClass

/**
 * 模组 ScreenHandler 类型访问入口。
 *
 * 使用 @ModScreenHandler 注解标记的 ScreenHandler 类会被 ClassScanner 自动扫描并注册。
 * 通过 [getType] 根据类获取对应的 [ScreenHandlerType]。
 */
object ModScreenHandlers {

    /**
     * 根据 ScreenHandler 类获取已注册的 [ScreenHandlerType]。
     * 仅对带 [ic2_120.registry.annotation.ModScreenHandler] 注解的类有效。
     */
    fun <T : ScreenHandler> getType(klass: KClass<T>): ScreenHandlerType<T> =
        ScreenHandlerTypeStore.getType(klass)
}

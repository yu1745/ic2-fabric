package ic2_120.registry

import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import kotlin.reflect.KClass

/**
 * 存储由 ClassScanner 扫描并注册的 ScreenHandlerType。
 * 供 [ModScreenHandlers][ic2_120.content.screen.ModScreenHandlers] 通过 [getType] 获取类型。
 */
object ScreenHandlerTypeStore {

    private val types = mutableMapOf<KClass<*>, ScreenHandlerType<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : ScreenHandler> getType(klass: KClass<T>): ScreenHandlerType<T> =
        types[klass] as? ScreenHandlerType<T>
            ?: error("ScreenHandlerType 未注册: ${klass.simpleName}，请确保该类已添加 @ModScreenHandler 注解")

    internal fun registerType(klass: KClass<*>, type: ScreenHandlerType<*>) {
        types[klass] = type
    }
}

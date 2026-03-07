package ic2_120.registry

import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import kotlin.reflect.KClass

/**
 * 存储由 ClassScanner 扫描并注册的 BlockEntityType。
 * 供 [ModBlockEntities][ic2_120.content.ModBlockEntities] 通过 [getType] 获取类型。
 */
object BlockEntityTypeStore {

    private val types = mutableMapOf<KClass<*>, BlockEntityType<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : BlockEntity> getType(klass: KClass<T>): BlockEntityType<T> =
        types[klass] as? BlockEntityType<T>
            ?: error("BlockEntityType 未注册: ${klass.simpleName}，请确保该类已添加 @ModBlockEntity 注解")

    internal fun registerType(klass: KClass<*>, type: BlockEntityType<*>) {
        types[klass] = type
    }
}

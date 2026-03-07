package ic2_120.content

import ic2_120.registry.BlockEntityTypeStore
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import kotlin.reflect.KClass

/**
 * 模组方块实体类型访问入口。
 *
 * 使用 @ModBlockEntity 注解标记的 BlockEntity 类会被 ClassScanner 自动扫描并注册。
 * 通过 [getType] 根据类获取对应的 [BlockEntityType]。
 *
 * 命名约定：方块实体注册名通常与对应方块一致（如 "electric_furnace"）。
 */
object ModBlockEntities {

    /**
     * 根据 BlockEntity 类获取已注册的 [BlockEntityType]。
     * 仅对带 [ic2_120.registry.annotation.ModBlockEntity] 注解的类有效。
     */
    fun <T : BlockEntity> getType(klass: KClass<T>): BlockEntityType<T> =
        BlockEntityTypeStore.getType(klass)
}

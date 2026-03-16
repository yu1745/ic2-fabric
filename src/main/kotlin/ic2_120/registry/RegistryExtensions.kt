package ic2_120.registry

import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier
import kotlin.reflect.KClass

// ========== Block 扩展 ==========

/**
 * 获取 Block 单例实例
 */
fun <T : Block> KClass<T>.instance(): T =
    ClassScanner.getBlockInstance(this) as T

/**
 * 获取 Block 的注册 ID
 */
@JvmName("blockId")
fun <T : Block> KClass<T>.id(): Identifier =
    Registries.BLOCK.getId(instance())

/**
 * 获取 Block 对应的 Item
 */
fun <T : Block> KClass<T>.item(): Item =
    instance().asItem()

// ========== Item 扩展 ==========

/**
 * 获取 Item 单例实例
 */
fun <T : Item> KClass<T>.instance(): T =
    ClassScanner.getItemInstance(this) as T

/**
 * 获取 Item 的注册 ID
 */
@JvmName("itemId")
fun <T : Item> KClass<T>.id(): Identifier =
    Registries.ITEM.getId(instance())

// ========== BlockEntity 扩展 ==========

/**
 * 获取 BlockEntityType
 */
fun <T : BlockEntity> KClass<T>.type(): BlockEntityType<T> =
    ClassScanner.getBlockEntityType(this)

// ========== ScreenHandler 扩展 ==========

/**
 * 获取 ScreenHandlerType
 */
fun <T : ScreenHandler> KClass<T>.type(): ScreenHandlerType<T> =
    ClassScanner.getScreenHandlerType(this)

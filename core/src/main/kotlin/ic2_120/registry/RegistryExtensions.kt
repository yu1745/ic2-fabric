package ic2_120.registry

import ic2_120.Ic2_120
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.item.Item
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.KClass

// ========== Block 扩展 ==========

/**
 * 获取 Block 单例实例
 */
@Suppress("UNCHECKED_CAST") // ClassScanner 以 Block::class 为 key 存储，调用方保证类型一致
fun <T : Block> KClass<T>.instance(): T =
    ClassScanner.getBlockInstance(this) as T

/**
 * 获取 Block 的注册 ID
 */
@JvmName("blockId")
fun <T : Block> KClass<T>.id(): ResourceLocation =
    BuiltInRegistries.BLOCK.getId(instance())

/**
 * 获取 Block 对应的 Item
 */
fun <T : Block> KClass<T>.item(): Item =
    instance().asItem()

// ========== Item 扩展 ==========

/**
 * 获取 Item 单例实例
 */
@Suppress("UNCHECKED_CAST") // 同上：ClassScanner 以 Item::class 为 key 存储，调用方保证类型一致
fun <T : Item> KClass<T>.instance(): T =
    ClassScanner.getItemInstance(this) as T

/**
 * 获取 Item 的注册 ID
 */
@JvmName("itemId")
fun <T : Item> KClass<T>.id(): ResourceLocation =
    BuiltInRegistries.ITEM.getId(instance())

/**
 * 生成配方 ID（带可选后缀）
 * 用于区分同一物品的多个配方
 */
@JvmName("itemRecipeId")
fun <T : Item> KClass<T>.recipeId(suffix: String = ""): ResourceLocation {
    val baseId = BuiltInRegistries.ITEM.getId(instance()).path
    return ResourceLocation.of(Ic2_120.MOD_ID, if (suffix.isNotEmpty()) "${baseId}_$suffix" else baseId)
}

/**
 * 生成配方 ID（带可选后缀）
 * 用于区分同一方块的多个配方
 */
@JvmName("blockRecipeId")
fun <T : Block> KClass<T>.recipeId(suffix: String = ""): ResourceLocation {
    val baseId = BuiltInRegistries.BLOCK.getId(instance()).path
    return ResourceLocation.of(Ic2_120.MOD_ID, if (suffix.isNotEmpty()) "${baseId}_$suffix" else baseId)
}

// ========== BlockEntity 扩展 ==========

/**
 * 获取 BlockEntityType
 */
fun <T : BlockEntity> KClass<T>.type(): BlockEntityType<T> =
    ClassScanner.getBlockEntityType(this)

// ========== AbstractContainerMenu 扩展 ==========

/**
 * 获取 MenuType
 */
fun <T : AbstractContainerMenu> KClass<T>.type(): MenuType<T> =
    ClassScanner.getScreenHandlerType(this)
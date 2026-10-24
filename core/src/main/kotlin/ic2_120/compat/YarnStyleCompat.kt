package ic2_120.compat

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.inventory.Slot
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * yarn→mojang 迁移兼容垫片：把 yarn 时代的高频属性名以扩展属性形式提供给 MC 类型。
 * 只在本仓库内部使用；不要在 shim 之外再起第二套名字。
 *
 * 背景：mojmap getter 与 yarn 不同名（getWorld→getLevel、getPos→getBlockPos、
 * isClient→isClientSide、getCachedState→getBlockState），Kotlin 合成属性随之改名。
 * 用 shim 保住旧属性名，避免全仓 4000+ 处机械改名引入错误。
 */
val BlockEntity.world: Level?
    get() = this.level

val BlockEntity.pos: BlockPos
    get() = this.blockPos

val BlockEntity.cachedState: BlockState
    get() = this.blockState

val Level.isClient: Boolean
    get() = this.isClientSide

val Level.isServer: Boolean
    get() = !this.isClientSide

val ItemEntity.stack: ItemStack
    get() = this.item

val BlockPlaceContext.stack: ItemStack
    get() = this.itemInHand

val Slot.stack: ItemStack
    get() = this.getItem()

val Level.time: Long
    get() = this.gameTime

/**
 * yarn `validateTicker(world, given, expected, ticker)` 的等价物
 * （mojmap 1.21.1 无此静态 helper；BaseEntityBlock.createTickerHelper 仅限其子类）。
 */
fun <E : BlockEntity, A : BlockEntity> validateTicker(
    givenType: BlockEntityType<A>, expectedType: BlockEntityType<E>, ticker: BlockEntityTicker<in E>
): BlockEntityTicker<A>? =
    if (givenType == expectedType) ticker as BlockEntityTicker<A> else null

/**
 * yarn `ItemStack.orCreateNbt` 的组件时代等价物。
 * ⚠️ 与 yarn 不同，NBT 是复制语义：修改后必须重新赋值（`stack.orCreateNbt = ...`），
 * 直接对 getter 结果 put/putXXX 不会写回。
 */
var ItemStack.orCreateNbt: CompoundTag
    get() = this.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
    set(value) {
        this.set(DataComponents.CUSTOM_DATA, CustomData.of(value))
    }

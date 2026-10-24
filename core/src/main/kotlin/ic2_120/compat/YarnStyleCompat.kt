package ic2_120.compat

import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

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

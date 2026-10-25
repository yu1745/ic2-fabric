package ic2_120.content.fluid

import net.minecraft.core.registries.Registries

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.material.FluidState
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.server.MinecraftServer
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.util.Mth
import net.minecraft.world.level.Level

/**
 * UU 物质液池效果：实体**任意部位接触** UU 物质流体时，每 1 秒（20 tick）获得一次生命回复 I。
 *
 * 判定方式：
 * - 遍历实体 boundingBox 覆盖的所有方块网格（与原版 [net.minecraft.world.entity.Entity.updateMovementInFluid]
 *   的网格遍历一致），任一格的 [FluidState] 属于 [UU_MATTER] tag 即视为"接触"。
 *   这覆盖了"脚踩在流体表面""身体侧面碰到流动的 UU 物质"等情况，比 `isSubmergedIn`（仅看眼部一格）宽松。
 * - 必须用 FluidState.`is`(tag) 自己扫描，不能用 entity.isSubmergedIn(tag) / fluidHeight：
 *   fluidHeight 只对硬编码的 FluidTags.LAVA / WATER 累加，自定义 tag 恒为 0；isSubmergedIn 只看眼部。
 * - 效果用 MobEffect（而非直接 heal），带原生粒子/图标，可被牛奶清除，符合 MC 习惯。
 *   每次给 60 tick（3 秒）Regeneration I，每秒续杯一次，离开液池后自然衰减。
 */
object UUFluidEffectHandler {

    /** UU 物质流体专属 tag（both still + flowing），由 ModFluidTagProvider datagen 生成。 */
    val UU_MATTER: TagKey<net.minecraft.world.level.material.Fluid> =
        TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("ic2_120", "uu_matter"))

    private var tickCounter = 0

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tick(server)
        }
    }

    private fun tick(server: MinecraftServer) {
        tickCounter++
        if (tickCounter % 20 != 0) return  // 每 1 秒一次

        for (world in server.allLevels) {
            for (entity in world.allEntities) {
                if (entity !is LivingEntity) continue
                if (!entity.isAlive) continue
                if (entity.isSpectator) continue
                if (!isTouchingUUMatter(world, entity.boundingBox)) continue
                // Regeneration I，60 tick（3 秒），环境粒子、显示 HUD 图标、可被打断
                entity.addEffect(
                    MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true)
                )
            }
        }
    }

    /**
     * 判定 [box] 覆盖的任一方块网格是否为 UU 物质流体。
     * 网格遍历方式与原版 `Entity.updateMovementInFluid` 一致（floor(maxY) 取上界，
     * 因此 2 格高的实体站进 1 格流体也能命中）。
     */
    private fun isTouchingUUMatter(world: Level, box: AABB): Boolean {
        val minX = Mth.floor(box.minX)
        val maxX = Mth.ceil(box.maxX)
        val minY = Mth.floor(box.minY)
        val maxY = Mth.ceil(box.maxY)
        val minZ = Mth.floor(box.minZ)
        val maxZ = Mth.ceil(box.maxZ)
        val pos = BlockPos.MutableBlockPos()

        for (x in minX until maxX) {
            for (y in minY until maxY) {
                for (z in minZ until maxZ) {
                    pos.set(x, y, z)
                    if (world.getFluidState(pos).`is`(UU_MATTER)) return true
                }
            }
        }
        return false
    }
}

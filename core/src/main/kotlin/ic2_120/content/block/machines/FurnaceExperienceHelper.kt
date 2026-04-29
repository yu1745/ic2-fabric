package ic2_120.content.block.machines

import net.minecraft.entity.ExperienceOrbEntity
import net.minecraft.recipe.AbstractCookingRecipe
import net.minecraft.recipe.Recipe
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

/**
 * 熔炉经验工具：提供经验累加和掉落逻辑，供铁炉/电炉复用。
 */
object FurnaceExperienceHelper {
    const val NBT_EXPERIENCE = "StoredExperience"

    /**
     * 在指定位置掉落经验球。
     * 经验总量为浮点数，整数部分直接掉落，小数部分按概率进位（与原版一致）。
     */
    fun dropExperience(world: ServerWorld, pos: BlockPos, totalExperience: Float) {
        if (totalExperience <= 0f) return
        val intPart = MathHelper.floor(totalExperience)
        val fracPart = MathHelper.fractionalPart(totalExperience)
        var amount = intPart
        if (fracPart != 0.0f && Math.random() < fracPart) amount++
        if (amount > 0) ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), amount)
    }

    /**
     * 从烧制配方中获取经验值。
     */
    fun getExperienceFromRecipe(recipe: Recipe<*>?): Float {
        if (recipe is AbstractCookingRecipe) return recipe.experience
        return 0f
    }
}

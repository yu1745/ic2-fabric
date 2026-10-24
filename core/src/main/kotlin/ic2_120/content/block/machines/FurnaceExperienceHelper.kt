package ic2_120.content.block.machines

import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

/**
 * 熔炉经验工具：提供经验累加和掉落逻辑，供铁炉/电炉复用。
 */
object FurnaceExperienceHelper {
    const val NBT_EXPERIENCE = "StoredExperience"

    /**
     * 在指定位置掉落经验球。
     * 经验总量为浮点数，整数部分直接掉落，小数部分按概率进位（与原版一致）。
     */
    fun dropExperience(world: ServerLevel, pos: BlockPos, totalExperience: Float) {
        if (totalExperience <= 0f) return
        val intPart = Mth.floor(totalExperience)
        val fracPart = Mth.frac(totalExperience)
        var amount = intPart
        if (fracPart != 0.0f && Math.random() < fracPart) amount++
        if (amount > 0) ExperienceOrb.award(world, Vec3.atCenterOf(pos), amount)
    }

    /**
     * 从烧制配方中获取经验值。
     */
    fun getExperienceFromRecipe(recipe: Recipe<*>?): Float {
        if (recipe is AbstractCookingRecipe) return recipe.experience
        return 0f
    }
}

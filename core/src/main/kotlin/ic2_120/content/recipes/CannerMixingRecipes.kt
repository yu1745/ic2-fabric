package ic2_120.content.recipes

import ic2_120.content.fluid.ModFluids
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 流体/固体装罐机的流体+固体混合配方。
 * 左侧储罐流体 + 中间固体材料 → 右侧储罐混合流体
 *
 * 已知配方：
 * - 水 + 8×青金石粉 → 冷却液
 * - 蒸馏水 + 1×青金石粉 → 冷却液
 * - 水 + 1×糠 → 生物质
 * - 水 + 建筑泡沫粉 → 建筑泡沫
 * - 水 + 1×蛤蛤粉 → 除草剂（weed_ex）
 */
object CannerMixingRecipes {

    data class Recipe(
        val inputFluid: Fluid,
        val inputSolid: IngredientInput,
        val inputSolidCount: Int,
        val outputFluid: Fluid,
        val inputFluidBuckets: Int = 1
    )

    private val lapisDust by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "lapis_dust")) }
    private val bioChaff by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "bio_chaff")) }
    private val cfPowder by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "cf_powder")) }
    private val grinPowder by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "grin_powder")) }
    private val peatOre by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "peat_ore")) }

    private val recipes: List<Recipe> by lazy {
        listOf(
            Recipe(Fluids.WATER, IngredientInput.tag(ModTags.Compat.Items.DUSTS_LAPIS, lapisDust), 8, ModFluids.COOLANT_STILL),
            Recipe(Fluids.FLOWING_WATER, IngredientInput.tag(ModTags.Compat.Items.DUSTS_LAPIS, lapisDust), 8, ModFluids.COOLANT_STILL),
            Recipe(ModFluids.DISTILLED_WATER_STILL, IngredientInput.tag(ModTags.Compat.Items.DUSTS_LAPIS, lapisDust), 1, ModFluids.COOLANT_STILL),
            Recipe(ModFluids.DISTILLED_WATER_FLOWING, IngredientInput.tag(ModTags.Compat.Items.DUSTS_LAPIS, lapisDust), 1, ModFluids.COOLANT_STILL),
            Recipe(Fluids.WATER, IngredientInput.item(bioChaff), 1, ModFluids.BIOMASS_STILL),
            Recipe(Fluids.FLOWING_WATER, IngredientInput.item(bioChaff), 1, ModFluids.BIOMASS_STILL),
            Recipe(Fluids.WATER, IngredientInput.item(cfPowder), 1, ModFluids.CONSTRUCTION_FOAM_STILL),
            Recipe(Fluids.FLOWING_WATER, IngredientInput.item(cfPowder), 1, ModFluids.CONSTRUCTION_FOAM_STILL),
            Recipe(Fluids.WATER, IngredientInput.item(grinPowder), 1, ModFluids.WEED_EX_STILL),
            Recipe(Fluids.FLOWING_WATER, IngredientInput.item(grinPowder), 1, ModFluids.WEED_EX_STILL),
            Recipe(Fluids.WATER, IngredientInput.item(peatOre), 1, ModFluids.BIOMASS_STILL),
            Recipe(Fluids.FLOWING_WATER, IngredientInput.item(peatOre), 1, ModFluids.BIOMASS_STILL)
        )
    }

    /**
     * 获取匹配的混合配方。
     * @param leftTankFluid 左侧储罐流体
     * @param leftTankAmount 左侧储罐流体量（Fabric 单位）
     * @param materialStack 材料槽物品
     * @return 若匹配则返回 Recipe，否则 null
     */
    fun getRecipe(leftTankFluid: Fluid?, leftTankAmount: Long, materialStack: ItemStack): Recipe? {
        if (materialStack.isEmpty || leftTankFluid == null || leftTankAmount < FluidConstants.BUCKET) return null
        return recipes.find { recipe ->
            fluidsMatch(recipe.inputFluid, leftTankFluid) &&
            recipe.inputSolid.toIngredient().test(materialStack) &&
            materialStack.count >= recipe.inputSolidCount
        }
    }

    /** 返回所有混合配方（去重） */
    fun allRecipes(): List<Recipe> = recipes

    /** 检查物品是否为混合配方可用材料 */
    fun isMixingMaterial(item: Item): Boolean =
        recipes.any { it.inputSolid.toIngredient().test(ItemStack(item)) }

    private fun fluidsMatch(recipeFluid: Fluid, tankFluid: Fluid): Boolean {
        if (recipeFluid == tankFluid) return true
        return when (tankFluid) {
            Fluids.FLOWING_WATER -> recipeFluid == Fluids.WATER
            Fluids.FLOWING_LAVA -> recipeFluid == Fluids.LAVA
            ModFluids.DISTILLED_WATER_FLOWING -> recipeFluid == ModFluids.DISTILLED_WATER_STILL
            ModFluids.BIOMASS_FLOWING -> recipeFluid == ModFluids.BIOMASS_STILL
            ModFluids.COOLANT_FLOWING -> recipeFluid == ModFluids.COOLANT_STILL
            ModFluids.CONSTRUCTION_FOAM_FLOWING -> recipeFluid == ModFluids.CONSTRUCTION_FOAM_STILL
            else -> false
        }
    }
}

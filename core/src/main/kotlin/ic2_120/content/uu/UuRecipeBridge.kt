package ic2_120.content.uu

import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipe
import ic2_120.content.recipes.blockcutter.BlockCutterRecipe
import ic2_120.content.recipes.centrifuge.CentrifugeRecipe
import ic2_120.content.recipes.compressor.CompressorRecipe
import ic2_120.content.recipes.extractor.ExtractorRecipe
import ic2_120.content.recipes.macerator.MaceratorRecipe
import ic2_120.content.recipes.metalformer.MetalFormerRecipe
import ic2_120.content.recipes.orewashing.OreWashingRecipe
import ic2_120.content.recipes.solidcanner.SolidCannerRecipe
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.AbstractCookingRecipe
import net.minecraft.world.item.crafting.SingleItemRecipe as McCuttingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

/**
 * 把 1.20.1 的 [RecipeManager] 里所有配方翻译成 [UuGraph.Transformation]。
 *
 * 对应 exp 的各 `IRecipeResolver`：工作台 / 熔炉 / 打粉 / 压缩 / 萃取 / 离心 / 切割 / 爆破 / 洗矿 / 装罐 / 金属成型。
 * 不含：回收机（概率产出）、废料盒（概率产出）——这两个需要特殊处理，先跳过保证主链正确。
 */
object UuRecipeBridge {

    private val logger = LoggerFactory.getLogger("ic2_120.uu.bridge")

    /**
     * 从 [RecipeManager] 收集所有 [UuGraph.Transformation]。
     * 返回 transformations 列表 + 诊断统计。
     */
    fun collect(recipeManager: net.minecraft.world.item.crafting.RecipeManager, registryManager: net.minecraft.core.RegistryAccess): Result {
        val transformations = mutableListOf<UuGraph.Transformation>()
        var skippedEmpty = 0
        var skippedAir = 0
        var crafting = 0
        var smelting = 0
        var stonecutting = 0
        var machine = 0

        for (holder in recipeManager.recipes) {
            val recipe = holder.value
            try {
                val tx = when (recipe) {
                    // ===== IC2 机器配方 =====
                    is MaceratorRecipe -> recipe.toTx()
                    is CompressorRecipe -> recipe.toTx()
                    is ExtractorRecipe -> recipe.toTx()
                    is CentrifugeRecipe -> recipe.toTx()
                    is BlockCutterRecipe -> recipe.toTx()
                    is BlastFurnaceRecipe -> recipe.toTx()
                    is OreWashingRecipe -> recipe.toTx()
                    is SolidCannerRecipe -> recipe.toTx()
                    is MetalFormerRecipe -> recipe.toMetalFormerTx()

                    // ===== 原版配方（AbstractCookingRecipe 含 Smelting/Smoking/Blasting）=====
                    is AbstractCookingRecipe -> {
                        smelting++
                        recipe.toSmeltingTx(registryManager)
                    }
                    is McCuttingRecipe -> {
                        // 含 StonecuttingRecipe
                        stonecutting++
                        recipe.toStonecuttingTx(registryManager)
                    }
                    is ShapedRecipe -> {
                        crafting++
                        recipe.toCraftingTx(registryManager)
                    }
                    is ShapelessRecipe -> {
                        crafting++
                        recipe.toCraftingTx(registryManager)
                    }
                    else -> null
                }
                when {
                    tx == null -> {} // 未支持的类型，跳过
                    tx.inputs.isEmpty() -> skippedEmpty++
                    tx.outputs.isEmpty() -> skippedEmpty++
                    tx.outputs.any { it.item === Items.AIR } -> skippedAir++
                    tx.inputs.any { group -> group.isEmpty() } -> skippedEmpty++
                    else -> {
                        transformations.add(tx)
                        if (recipe !is ShapedRecipe && recipe !is ShapelessRecipe &&
                            recipe !is AbstractCookingRecipe && recipe !is McCuttingRecipe
                        ) machine++
                    }
                }
            } catch (e: Exception) {
                logger.warn("跳过配方 ${holder.id()}: ${e.message}")
            }
        }

        return Result(
            transformations = transformations,
            craftingCount = crafting,
            smeltingCount = smelting,
            stonecuttingCount = stonecutting,
            machineCount = machine,
            skippedEmpty = skippedEmpty,
            skippedAir = skippedAir
        )
    }

    data class Result(
        val transformations: List<UuGraph.Transformation>,
        val craftingCount: Int,
        val smeltingCount: Int,
        val stonecuttingCount: Int,
        val machineCount: Int,
        val skippedEmpty: Int,
        val skippedAir: Int
    )

    // ===== IC2 机器配方适配 =====

    private fun MaceratorRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(inputCount)),
            outputs = listOf(output.toLean())
        )

    private fun CompressorRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(inputCount)),
            outputs = listOf(output.toLean()) + containerReturn.takeIf { !it.isEmpty }?.toLean()?.let { listOf(it) }.orEmpty()
        )

    private fun ExtractorRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(1)),
            outputs = listOf(output.toLean())
        )

    private fun CentrifugeRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(inputCount)),
            outputs = outputs.map { it.toLean() }
        )

    private fun BlockCutterRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(inputCount)),
            outputs = listOf(output.toLean())
        )

    private fun BlastFurnaceRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(1)),
            outputs = listOf(steelOutput.toLean(), slagOutput.toLean())
        )

    private fun OreWashingRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(1)),
            outputs = outputItems.map { it.toLean() }
        )

    private fun SolidCannerRecipe.toTx(): UuGraph.Transformation =
        UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(
                slot0Ingredient.toLeanGroup(slot0Count),
                slot1Ingredient.toLeanGroup(slot1Count)
            ),
            outputs = listOf(output.toLean())
        )

    private fun MetalFormerRecipe.toMetalFormerTx(): UuGraph.Transformation {
        // MetalFormer sealed class：ingredient/output 是公开 val，子类 Rolling/Cutting/Extruding 直接继承
        return UuGraph.Transformation(
            transformCost = UuGraph.MACHINE_COST,
            inputs = listOf(ingredient.toLeanGroup(1)),
            outputs = listOf(output.toLean())
        )
    }

    // ===== 原版配方适配 =====

    private fun AbstractCookingRecipe.toSmeltingTx(rm: net.minecraft.core.RegistryAccess): UuGraph.Transformation? {
        // AbstractCookingRecipe.getIngredients() (public) 返回单元素 list，getOutput(rm) 返回输出
        val ingredientList = this.ingredients
        if (ingredientList.isEmpty()) return null
        val out = this.getResultItem(rm)
        if (out.isEmpty) return null
        val groups = ingredientList.map { it.toLeanGroup(1) }.filter { it.isNotEmpty() }
        if (groups.isEmpty()) return null
        return UuGraph.Transformation(
            transformCost = UuGraph.SMELTING_COST,
            inputs = groups,
            outputs = listOf(out.toLean())
        )
    }

    private fun McCuttingRecipe.toStonecuttingTx(rm: net.minecraft.core.RegistryAccess): UuGraph.Transformation? {
        val ingredientList = this.ingredients
        if (ingredientList.isEmpty()) return null
        val out = this.getResultItem(rm)
        val groups = ingredientList.map { it.toLeanGroup(1) }.filter { it.isNotEmpty() }
        if (groups.isEmpty()) return null
        return UuGraph.Transformation(
            transformCost = UuGraph.CRAFTING_COST,
            inputs = groups,
            outputs = if (out.isEmpty) emptyList() else listOf(out.toLean())
        )
    }

    private fun ShapedRecipe.toCraftingTx(rm: net.minecraft.core.RegistryAccess): UuGraph.Transformation? =
        toCraftingTxCommon(rm)

    private fun ShapelessRecipe.toCraftingTx(rm: net.minecraft.core.RegistryAccess): UuGraph.Transformation? =
        toCraftingTxCommon(rm)

    private fun Recipe<*>.toCraftingTxCommon(rm: net.minecraft.core.RegistryAccess): UuGraph.Transformation? {
        val out = this.getResultItem(rm)
        if (out.isEmpty) return null
        val ingredientList = this.ingredients
        if (ingredientList.isEmpty()) return null
        // 过滤空槽位（pattern 里的空格产生 Ingredient.empty，matchingStacks 为空）
        val groups = ingredientList.map { it.toLeanGroup(1) }.filter { it.isNotEmpty() }
        if (groups.isEmpty()) return null
        return UuGraph.Transformation(
            transformCost = UuGraph.CRAFTING_COST,
            inputs = groups,
            outputs = listOf(out.toLean())
        )
    }

    // ===== 工具：Ingredient / ItemStack -> LeanStack =====

    /**
     * 一个 [Ingredient] 转成一个「OR 备选组」：每种匹配的物品各一个 [UuGraph.LeanStack]。
     * [count] 为该输入槽位需要的物品数量（每个备选都按此数量）。
     */
    private fun Ingredient.toLeanGroup(count: Int): List<UuGraph.LeanStack> {
        val stacks = this.items
        if (stacks.isEmpty()) return emptyList()
        // 按 Item 去重（同 item 不同 nbt 只取第一个，UU 计算不关心 NBT）
        val seen = mutableSetOf<Item>()
        val result = mutableListOf<UuGraph.LeanStack>()
        for (s in stacks) {
            if (s.isEmpty || s.item === Items.AIR) continue
            if (seen.add(s.item)) {
                result.add(UuGraph.LeanStack(s.item, count))
            }
        }
        return result
    }

    private fun Array<ItemStack>.toLeanGroup(count: Int): List<UuGraph.LeanStack> {
        val seen = mutableSetOf<Item>()
        val result = mutableListOf<UuGraph.LeanStack>()
        for (s in this) {
            if (s.isEmpty || s.item === Items.AIR) continue
            if (seen.add(s.item)) {
                result.add(UuGraph.LeanStack(s.item, count))
            }
        }
        return result
    }

    private fun List<ItemStack>.toLeanGroup(count: Int): List<UuGraph.LeanStack> =
        toTypedArray().toLeanGroup(count)

    private fun ItemStack.toLean(): UuGraph.LeanStack =
        UuGraph.LeanStack(this.item, this.count.coerceAtLeast(1))
}

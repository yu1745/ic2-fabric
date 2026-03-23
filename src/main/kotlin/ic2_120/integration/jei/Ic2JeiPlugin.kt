package ic2_120.integration.jei

import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipeDatagen
import ic2_120.content.recipes.blockcutter.BlockCutterRecipeDatagen
import ic2_120.content.recipes.centrifuge.CentrifugeRecipeDatagen
import ic2_120.content.recipes.compressor.CompressorRecipeDatagen
import ic2_120.content.recipes.extractor.ExtractorRecipeDatagen
import ic2_120.content.recipes.macerator.MaceratorRecipeDatagen
import ic2_120.content.recipes.metalformer.MetalFormerRecipeDatagen
import ic2_120.content.recipes.orewashing.OreWashingRecipeDatagen
import ic2_120.integration.jei.BlastFurnaceJeiRecipe
import ic2_120.integration.jei.BlastFurnaceRecipeCategory
import ic2_120.integration.jei.BlockCutterJeiRecipe
import ic2_120.integration.jei.BlockCutterRecipeCategory
import ic2_120.integration.jei.MetalFormerCuttingJeiRecipe
import ic2_120.integration.jei.MetalFormerCuttingRecipeCategory
import ic2_120.integration.jei.MetalFormerExtrudingJeiRecipe
import ic2_120.integration.jei.MetalFormerExtrudingRecipeCategory
import ic2_120.integration.jei.MetalFormerRollingJeiRecipe
import ic2_120.integration.jei.MetalFormerRollingRecipeCategory
import ic2_120.integration.jei.OreWashingJeiRecipe
import ic2_120.integration.jei.OreWashingRecipeCategory
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter
import mezz.jei.api.registration.IExtraIngredientRegistration
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.registration.ISubtypeRegistration
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * JEI 插件 - 注册电力物品的空电和满电变体
 *
 * 此插件会在 JEI 中显示电力物品的空电和满电版本，
 * 与创造模式物品栏中的显示保持一致。
 */
@JeiPlugin
class Ic2JeiPlugin : IModPlugin {
    override fun getPluginUid(): Identifier {
        return Identifier("ic2_120", "main")
    }

    override fun registerItemSubtypes(registration: ISubtypeRegistration) {
        // 遍历所有已注册的物品
        Registries.ITEM.forEach { item ->
            // 检查物品是否实现了电力接口
            if (item is IBatteryItem || item is IElectricTool) {
                // 注册 NBT 子类型解释器
                registration.registerSubtypeInterpreter(
                    item,
                    ElectricItemSubtypeInterpreter()
                )
            }
            // 检查物品是否为喷气背包
            if (item is JetpackItem) {
                registration.registerSubtypeInterpreter(
                    item,
                    JetpackItemSubtypeInterpreter()
                )
            }
        }
    }

    override fun registerExtraIngredients(registration: IExtraIngredientRegistration) {
        val extraStacks = mutableListOf<ItemStack>()

        Registries.ITEM.forEach { item ->
            when (item) {
                is IBatteryItem -> {
                    // 电池：补充空电 + 满电两个明确变体
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setCurrentCharge(stack, 0L)
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setCurrentCharge(stack, item.maxCapacity)
                    }
                }

                is IElectricTool -> {
                    // 电动工具：补充空电 + 满电两个明确变体
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setEnergy(stack, 0L)
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setEnergy(stack, item.maxCapacity)
                    }
                }

                is JetpackItem -> {
                    // 喷气背包：补充空燃料 + 满燃料两个明确变体
                    extraStacks += ItemStack(item as Item).also { stack ->
                        JetpackItem.setFuel(stack, 0L)
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        JetpackItem.setFuel(stack, JetpackItem.MAX_FUEL)
                    }
                }
            }
        }

        if (extraStacks.isNotEmpty()) {
            registration.addExtraItemStacks(extraStacks)
        }
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(
            MaceratorRecipeCategory(registration.jeiHelpers.guiHelper),
            CompressorRecipeCategory(registration.jeiHelpers.guiHelper),
            ExtractorRecipeCategory(registration.jeiHelpers.guiHelper),
            CentrifugeRecipeCategory(registration.jeiHelpers.guiHelper),
            BlastFurnaceRecipeCategory(registration.jeiHelpers.guiHelper),
            OreWashingRecipeCategory(registration.jeiHelpers.guiHelper),
            BlockCutterRecipeCategory(registration.jeiHelpers.guiHelper),
            MetalFormerRollingRecipeCategory(registration.jeiHelpers.guiHelper),
            MetalFormerCuttingRecipeCategory(registration.jeiHelpers.guiHelper),
            MetalFormerExtrudingRecipeCategory(registration.jeiHelpers.guiHelper)
        )
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        // Macerator 配方
        val maceratorRecipes = MaceratorRecipeDatagen.allEntries()
            .map { entry ->
                MaceratorJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.MACERATOR, maceratorRecipes)

        // Compressor 配方
        val compressorRecipes = CompressorRecipeDatagen.allEntries()
            .map { entry ->
                CompressorJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    entry.inputCount,
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.COMPRESSOR, compressorRecipes)

        // Extractor 配方
        val extractorRecipes = ExtractorRecipeDatagen.allEntries()
            .map { entry ->
                ExtractorJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.EXTRACTOR, extractorRecipes)

        // Centrifuge 配方
        val centrifugeRecipes = CentrifugeRecipeDatagen.allEntries()
            .map { entry ->
                CentrifugeJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    entry.minHeat,
                    entry.outputs.map { ItemStack(it.item, it.count) }
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.CENTRIFUGE, centrifugeRecipes)

        // BlastFurnace 配方
        val blastFurnaceRecipes = BlastFurnaceRecipeDatagen.allEntries()
            .map { entry ->
                BlastFurnaceJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    ItemStack(entry.steelOutput, entry.steelCount),
                    ItemStack(entry.slagOutput, entry.slagCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.BLAST_FURNACE, blastFurnaceRecipes)

        // OreWashing 配方
        val oreWashingRecipes = OreWashingRecipeDatagen.allEntries()
            .map { entry ->
                OreWashingJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    entry.outputs.map { ItemStack(it.item, it.count) },
                    entry.waterConsumptionMb
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.ORE_WASHING, oreWashingRecipes)

        // BlockCutter 配方
        val blockCutterRecipes = BlockCutterRecipeDatagen.allEntries()
            .map { entry ->
                BlockCutterJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    entry.inputCount,
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.BLOCK_CUTTER, blockCutterRecipes)

        // MetalFormer 配方 - 按模式分组
        val metalFormerRecipes = MetalFormerRecipeDatagen.allEntries()
        val rollingRecipes = metalFormerRecipes.filter { it.mode == MetalFormerRecipeDatagen.Mode.ROLLING }
            .map { entry ->
                MetalFormerRollingJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    ItemStack(entry.output, entry.outputCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.METAL_FORMER_ROLLING, rollingRecipes)

        val cuttingRecipes = metalFormerRecipes.filter { it.mode == MetalFormerRecipeDatagen.Mode.CUTTING }
            .map { entry ->
                MetalFormerCuttingJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    ItemStack(entry.output, entry.outputCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.METAL_FORMER_CUTTING, cuttingRecipes)

        val extrudingRecipes = metalFormerRecipes.filter { it.mode == MetalFormerRecipeDatagen.Mode.EXTRUDING }
            .map { entry ->
                MetalFormerExtrudingJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    ItemStack(entry.output, entry.outputCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.METAL_FORMER_EXTRUDING, extrudingRecipes)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        // Macerator
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "macerator"))),
            Ic2JeiRecipeTypes.MACERATOR
        )

        // Compressor
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "compressor"))),
            Ic2JeiRecipeTypes.COMPRESSOR
        )

        // Extractor
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "extractor"))),
            Ic2JeiRecipeTypes.EXTRACTOR
        )

        // Centrifuge
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "centrifuge"))),
            Ic2JeiRecipeTypes.CENTRIFUGE
        )

        // BlockCutter
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "block_cutter"))),
            Ic2JeiRecipeTypes.BLOCK_CUTTER
        )

        // BlastFurnace
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "blast_furnace"))),
            Ic2JeiRecipeTypes.BLAST_FURNACE
        )

        // OreWashing
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "ore_washing_plant"))),
            Ic2JeiRecipeTypes.ORE_WASHING
        )

        // MetalFormer - 同一机器显示在三个分类下
        val metalFormerStack = ItemStack(Registries.ITEM.get(Identifier("ic2_120", "metal_former")))
        registration.addRecipeCatalyst(metalFormerStack, Ic2JeiRecipeTypes.METAL_FORMER_ROLLING)
        registration.addRecipeCatalyst(metalFormerStack, Ic2JeiRecipeTypes.METAL_FORMER_CUTTING)
        registration.addRecipeCatalyst(metalFormerStack, Ic2JeiRecipeTypes.METAL_FORMER_EXTRUDING)
    }

    /**
     * 电力物品 NBT 子类型解释器
     *
     * 使用 "Energy" NBT 标签来区分不同电量状态的物品。
     * JEI 会根据此解释器识别不同的 ItemStack 为独立的物品。
     */
    class ElectricItemSubtypeInterpreter : IIngredientSubtypeInterpreter<ItemStack> {
        companion object {
            /** 子类型标识符：空电版本 */
            private const val EMPTY_TAG = "empty"

            /** 子类型标识符：满电版本 */
            private const val FULL_TAG = "full"

            /** 子类型标识符：部分电量 */
            private const val PARTIAL_TAG = "partial"
        }

        override fun apply(itemStack: ItemStack, uidContext: mezz.jei.api.ingredients.subtypes.UidContext): String {
            // 通过接口读取电量，避免依赖固定 NBT 键名
            val maxCapacity = when (val item = itemStack.item) {
                is IBatteryItem -> item.maxCapacity
                is IElectricTool -> item.maxCapacity
                else -> 1L
            }
            val energy = when (val item = itemStack.item) {
                is IBatteryItem -> item.getCurrentCharge(itemStack)
                is IElectricTool -> item.getEnergy(itemStack)
                else -> return ""
            }

            // 根据电量比例返回子类型标识符
            return when {
                energy <= 0 -> EMPTY_TAG
                energy >= maxCapacity -> FULL_TAG
                else -> "$PARTIAL_TAG:$energy"
            }
        }
    }

    /**
     * 喷气背包 NBT 子类型解释器
     *
     * 使用 "Fuel" NBT 标签来区分不同燃料状态的物品。
     * JEI 会根据此解释器识别不同的 ItemStack 为独立的物品。
     */
    class JetpackItemSubtypeInterpreter : IIngredientSubtypeInterpreter<ItemStack> {
        companion object {
            /** 子类型标识符：空燃料版本 */
            private const val EMPTY_TAG = "empty"

            /** 子类型标识符：满燃料版本 */
            private const val FULL_TAG = "full"

            /** 子类型标识符：部分燃料 */
            private const val PARTIAL_TAG = "partial"
        }

        override fun apply(itemStack: ItemStack, uidContext: mezz.jei.api.ingredients.subtypes.UidContext): String {
            val fuel = JetpackItem.getFuel(itemStack)

            // 根据燃料比例返回子类型标识符
            return when {
                fuel <= 0 -> EMPTY_TAG
                fuel >= JetpackItem.MAX_FUEL -> FULL_TAG
                else -> "$PARTIAL_TAG:$fuel"
            }
        }
    }
}

package ic2_120.integration.jei

import ic2_120.config.Ic2Config
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipeDatagen
import ic2_120.content.recipes.blockcutter.BlockCutterRecipeDatagen
import ic2_120.content.recipes.centrifuge.CentrifugeRecipeDatagen
import ic2_120.content.recipes.compressor.CompressorRecipeDatagen
import ic2_120.content.recipes.extractor.ExtractorRecipeDatagen
import ic2_120.content.recipes.macerator.MaceratorRecipeDatagen
import ic2_120.content.recipes.metalformer.MetalFormerRecipeDatagen
import ic2_120.content.recipes.orewashing.OreWashingRecipeDatagen
import ic2_120.content.item.EmptyTinCanItem
import ic2_120.content.item.FilledTinCanItem
import ic2_120.content.recipes.solidcanner.SolidCannerRecipeDatagen
import ic2_120.registry.instance
import net.minecraft.component.DataComponentTypes
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
import ic2_120.integration.jei.RecyclerJeiRecipe
import ic2_120.integration.jei.RecyclerRecipeCategory
import ic2_120.integration.jei.SolidCannerJeiRecipe
import ic2_120.integration.jei.SolidCannerRecipeCategory
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.CropSeedBagItem
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
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import ic2_120.editCustomData
import ic2_120.getCustomData

/**
 * JEI 插件 - 注册电力物品、喷气背包、建筑泡沫喷枪等的空/满变体与子类型
 *
 * 在 JEI 中补充空电与满电、空燃料与满燃料、喷枪空罐与满罐等条目，
 * 与创造模式物品栏（受配置开关控制的部分）展示思路一致。
 */
@JeiPlugin
class Ic2JeiPlugin : IModPlugin {
    companion object {
        @Volatile
        var jeiRuntime: IJeiRuntime? = null

        private var replicatorRecipes: List<ReplicatorJeiRecipe> = emptyList()

        fun refreshReplicatorRecipes() {
            val runtime = jeiRuntime ?: return
            val oldRecipes = replicatorRecipes
            if (oldRecipes.isNotEmpty()) {
                runtime.recipeManager.hideRecipes(Ic2JeiRecipeTypes.REPLICATOR, oldRecipes)
            }
            val newRecipes = Ic2Config.getAllReplicationCosts()
                .mapNotNull { (itemId, uuCostUb) ->
                    val item = Registries.ITEM.get(Identifier.tryParse(itemId))
                    if (item == Items.AIR) null
                    else ReplicatorJeiRecipe(ItemStack(item, 1), uuCostUb)
                }
            if (newRecipes.isNotEmpty()) {
                runtime.recipeManager.addRecipes(Ic2JeiRecipeTypes.REPLICATOR, newRecipes)
            }
            replicatorRecipes = newRecipes
        }
    }

    override fun getPluginUid(): Identifier {
        return Identifier.of("ic2_120", "main")
    }

    override fun registerItemSubtypes(registration: ISubtypeRegistration) {
        // 遍历所有已注册的物品
        Registries.ITEM.forEach { item ->
            if (item === Items.AIR) return@forEach
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
            if (item is FoamSprayerItem) {
                registration.registerSubtypeInterpreter(
                    item,
                    FoamSprayerSubtypeInterpreter()
                )
            }
            // 检查物品是否为储电盒/充电座 BlockItem
            if (item is EnergyStorageBlock.EnergyStorageBlockItem) {
                registration.registerSubtypeInterpreter(
                    item,
                    EnergyStorageBlockItemSubtypeInterpreter()
                )
            }
        }
    }

    override fun registerExtraIngredients(registration: IExtraIngredientRegistration) {
        val extraStacks = mutableListOf<ItemStack>()

        Registries.ITEM.forEach { item ->
            if (item === Items.AIR) return@forEach
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

                is FoamSprayerItem -> {
                    // 建筑泡沫喷枪：空罐 + 满罐（8 桶）
                    extraStacks += ItemStack(item as Item).also { stack ->
                        FoamSprayerItem.setFluidAmount(stack, 0L)
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        FoamSprayerItem.setFluidAmount(stack, FoamSprayerItem.CAPACITY_DROPLETS)
                    }
                }

                is EnergyStorageBlock.EnergyStorageBlockItem -> {
                    // 储电盒/充电座：补充空电 + 满电两个明确变体
                    extraStacks += ItemStack(item as Item).also { stack ->
                        stack.editCustomData { nbt -> nbt.putBoolean(EnergyStorageBlock.NBT_FULL, false) }
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        stack.editCustomData { nbt -> nbt.putBoolean(EnergyStorageBlock.NBT_FULL, true) }
                    }
                }
            }
        }

        // 注册储电盒/充电座的满电变体（通过物品 ID 直接获取）
        registerEnergyStorageFullVariants(extraStacks)
        // 注册杂交作物初始种子袋（1/1/1）
        extraStacks += CropSeedBagItem.createInitialSeedStacks()

        if (extraStacks.isNotEmpty()) {
            registration.addExtraItemStacks(extraStacks)
        }
    }

    override fun onRuntimeAvailable(runtime: IJeiRuntime) {
        Ic2JeiPlugin.jeiRuntime = runtime
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
            MetalFormerExtrudingRecipeCategory(registration.jeiHelpers.guiHelper),
            SolidCannerRecipeCategory(registration.jeiHelpers.guiHelper),
            RecyclerRecipeCategory(registration.jeiHelpers.guiHelper),
            ReplicatorRecipeCategory(registration.jeiHelpers.guiHelper)
        )
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        // Macerator 配方
        val maceratorRecipes = MaceratorRecipeDatagen.allEntries()
            .map { entry ->
                MaceratorJeiRecipe(
                    ItemStack(entry.input, entry.inputCount),
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.MACERATOR, maceratorRecipes)

        // Compressor 配方
        val compressorRecipes = CompressorRecipeDatagen.allEntries()
            .map { entry ->
                CompressorJeiRecipe(
                    ItemStack(entry.input, entry.inputCount),
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.COMPRESSOR, compressorRecipes)

        // Extractor 配方
        val extractorRecipes = ExtractorRecipeDatagen.allEntries()
            .map { entry ->
                ExtractorJeiRecipe(
                    ItemStack(entry.input, 1),
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.EXTRACTOR, extractorRecipes)

        // Centrifuge 配方
        val centrifugeRecipes = CentrifugeRecipeDatagen.allEntries()
            .map { entry ->
                CentrifugeJeiRecipe(
                    ItemStack(entry.input, entry.inputCount),
                    entry.minHeat,
                    entry.outputs.map { ItemStack(it.item, it.count) }
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.CENTRIFUGE, centrifugeRecipes)

        // BlastFurnace 配方
        val blastFurnaceRecipes = BlastFurnaceRecipeDatagen.allEntries()
            .map { entry ->
                BlastFurnaceJeiRecipe(
                    ItemStack(entry.input, 1),
                    ItemStack(entry.steelOutput, entry.steelCount),
                    ItemStack(entry.slagOutput, entry.slagCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.BLAST_FURNACE, blastFurnaceRecipes)

        // OreWashing 配方
        val oreWashingRecipes = OreWashingRecipeDatagen.allEntries()
            .map { entry ->
                OreWashingJeiRecipe(
                    ItemStack(entry.input, 1),
                    entry.outputs.map { ItemStack(it.item, it.count) },
                    entry.waterConsumptionMb
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.ORE_WASHING, oreWashingRecipes)

        // BlockCutter 配方
        val blockCutterRecipes = BlockCutterRecipeDatagen.allEntries()
            .map { entry ->
                BlockCutterJeiRecipe(
                    ItemStack(entry.input, entry.inputCount),
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.BLOCK_CUTTER, blockCutterRecipes)

        // MetalFormer 配方 - 按模式分组
        val metalFormerRecipes = MetalFormerRecipeDatagen.allEntries()
        val rollingRecipes = metalFormerRecipes.filter { it.mode == MetalFormerRecipeDatagen.Mode.ROLLING }
            .map { entry ->
                MetalFormerRollingJeiRecipe(
                    ItemStack(entry.input, 1),
                    ItemStack(entry.output, entry.outputCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.METAL_FORMER_ROLLING, rollingRecipes)

        val cuttingRecipes = metalFormerRecipes.filter { it.mode == MetalFormerRecipeDatagen.Mode.CUTTING }
            .map { entry ->
                MetalFormerCuttingJeiRecipe(
                    ItemStack(entry.input, 1),
                    ItemStack(entry.output, entry.outputCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.METAL_FORMER_CUTTING, cuttingRecipes)

        val extrudingRecipes = metalFormerRecipes.filter { it.mode == MetalFormerRecipeDatagen.Mode.EXTRUDING }
            .map { entry ->
                MetalFormerExtrudingJeiRecipe(
                    ItemStack(entry.input, 1),
                    ItemStack(entry.output, entry.outputCount)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.METAL_FORMER_EXTRUDING, extrudingRecipes)

        // SolidCanner 配方
        val tinCanStack = ItemStack(EmptyTinCanItem::class.instance(), 1)
        val filledCanStack = ItemStack(FilledTinCanItem::class.instance(), 1)
        val foodStacks = Registries.ITEM.mapNotNull { item ->
            if (ItemStack(item).get(DataComponentTypes.FOOD) != null) ItemStack(item, 1) else null
        }
        val solidCannerRecipes = SolidCannerRecipeDatagen.allEntries()
            .map { entry ->
                SolidCannerJeiRecipe(
                    slot0 = ItemStack(entry.slot0Ingredient, entry.slot0Count),
                    slot1 = listOf(ItemStack(entry.slot1Ingredient, entry.slot1Count)),
                    output = ItemStack(entry.outputItem, entry.outputCount)
                )
            } + SolidCannerJeiRecipe(tinCanStack, foodStacks, filledCanStack)
        registration.addRecipes(Ic2JeiRecipeTypes.SOLID_CANNER, solidCannerRecipes)

        val recyclerScrap = Registries.ITEM.get(Identifier.of("ic2_120", "scrap"))
        registration.addRecipes(
            Ic2JeiRecipeTypes.RECYCLER,
            listOf(
                RecyclerJeiRecipe(
                    ItemStack(Items.COBBLESTONE, 1),
                    ItemStack(recyclerScrap, 1)
                )
            )
        )

        // Replicator 配方
        replicatorRecipes = Ic2Config.getAllReplicationCosts()
            .mapNotNull { (itemId, uuCostUb) ->
                val item = Registries.ITEM.get(Identifier.tryParse(itemId))
                if (item == Items.AIR) null
                else ReplicatorJeiRecipe(ItemStack(item, 1), uuCostUb)
            }
        registration.addRecipes(Ic2JeiRecipeTypes.REPLICATOR, replicatorRecipes)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        // Macerator
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "macerator"))),
            Ic2JeiRecipeTypes.MACERATOR
        )

        // Compressor
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "compressor"))),
            Ic2JeiRecipeTypes.COMPRESSOR
        )

        // Extractor
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "extractor"))),
            Ic2JeiRecipeTypes.EXTRACTOR
        )

        // Centrifuge
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "centrifuge"))),
            Ic2JeiRecipeTypes.CENTRIFUGE
        )

        // BlockCutter
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "block_cutter"))),
            Ic2JeiRecipeTypes.BLOCK_CUTTER
        )

        // BlastFurnace
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "blast_furnace"))),
            Ic2JeiRecipeTypes.BLAST_FURNACE
        )

        // OreWashing
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "ore_washing_plant"))),
            Ic2JeiRecipeTypes.ORE_WASHING
        )

        // MetalFormer - 同一机器显示在三个分类下
        val metalFormerStack = ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "metal_former")))
        registration.addRecipeCatalyst(metalFormerStack, Ic2JeiRecipeTypes.METAL_FORMER_ROLLING)
        registration.addRecipeCatalyst(metalFormerStack, Ic2JeiRecipeTypes.METAL_FORMER_CUTTING)
        registration.addRecipeCatalyst(metalFormerStack, Ic2JeiRecipeTypes.METAL_FORMER_EXTRUDING)

        // SolidCanner
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "solid_canner"))),
            Ic2JeiRecipeTypes.SOLID_CANNER
        )

        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier.of("ic2_120", "recycler"))),
            Ic2JeiRecipeTypes.RECYCLER
        )

        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "replicator"))),
            Ic2JeiRecipeTypes.REPLICATOR
        )
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
            // 在配方匹配上下文中忽略电量细分，避免“部分电量”无法查到用途/配方。
            if (uidContext == mezz.jei.api.ingredients.subtypes.UidContext.Recipe) {
                return ""
            }

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
            return when {
                fuel <= 0 -> EMPTY_TAG
                fuel >= JetpackItem.MAX_FUEL -> FULL_TAG
                else -> "$PARTIAL_TAG:$fuel"
            }
        }
    }

    /**
     * 建筑泡沫喷枪：按 NBT 内流体滴数区分子类型（空 / 满 / 部分）。
     */
    class FoamSprayerSubtypeInterpreter : IIngredientSubtypeInterpreter<ItemStack> {
        companion object {
            private const val EMPTY_TAG = "empty"
            private const val FULL_TAG = "full"
            private const val PARTIAL_TAG = "partial"
        }

        override fun apply(itemStack: ItemStack, uidContext: mezz.jei.api.ingredients.subtypes.UidContext): String {
            val amt = FoamSprayerItem.getFluidAmount(itemStack)
            val cap = FoamSprayerItem.CAPACITY_DROPLETS
            return when {
                amt <= 0L -> EMPTY_TAG
                amt >= cap -> FULL_TAG
                else -> "$PARTIAL_TAG:$amt"
            }
        }
    }

    /**
     * 储电盒/充电座 NBT 子类型解释器
     *
     * 使用 "Full" NBT 标签来区分空电和满电状态的物品。
     * JEI 会根据此解释器识别不同的 ItemStack 为独立的物品。
     */
    class EnergyStorageBlockItemSubtypeInterpreter : IIngredientSubtypeInterpreter<ItemStack> {
        companion object {
            /** 子类型标识符：空电版本 */
            private const val EMPTY_TAG = "empty"

            /** 子类型标识符：满电版本 */
            private const val FULL_TAG = "full"
        }

        override fun apply(itemStack: ItemStack, uidContext: mezz.jei.api.ingredients.subtypes.UidContext): String {
            val nbt = itemStack.getCustomData() ?: return EMPTY_TAG
            return if (nbt.getBoolean(EnergyStorageBlock.NBT_FULL)) FULL_TAG else EMPTY_TAG
        }
    }

    /**
     * 注册储电盒/充电座物品的空电和满电变体。
     * 直接通过物品 ID 获取，绕过类层级检查。
     */
    private fun registerEnergyStorageFullVariants(extraStacks: MutableList<ItemStack>) {
        val storageIds = listOf(
            "batbox", "cesu", "mfe", "mfsu",
            "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad"
        )
        for (id in storageIds) {
            val item = Registries.ITEM.get(Identifier.of("ic2_120", id))
            if (item === net.minecraft.item.Items.AIR) continue
            // 空电
            extraStacks += ItemStack(item).also { stack ->
                stack.editCustomData { nbt -> nbt.putBoolean(EnergyStorageBlock.NBT_FULL, false) }
            }
            // 满电
            extraStacks += ItemStack(item).also { stack ->
                stack.editCustomData { nbt -> nbt.putBoolean(EnergyStorageBlock.NBT_FULL, true) }
            }
        }
    }
}

package ic2_120.content.item

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName
import ic2_120.content.reactor.AbstractDamageableReactorComponent
import ic2_120.content.reactor.AbstractFiniteNeutronReflectorItem
import ic2_120.content.reactor.AbstractReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.Identifier

// ========== 反应堆核心部件 ==========

/**
 * 冷凝器修复配方生成器
 */
private object CondensatorRepairRecipe {
    fun generate(exporter: RecipeOutput, condensator: Item, repairItem: Item, repairAmount: Int) {
        // 使用特殊配方类型，这里暂时用 ShapelessRecipeBuilder
        // 注意：这只是占位，实际修复逻辑需要在 CondensatorItem 中实现
        ShapelessRecipeBuilder.shapeless(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, condensator, 1)
            .requires(condensator)
            .requires(repairItem)
            .unlockedBy(
                net.minecraft.data.recipes.RecipeProvider.getHasName(condensator),
                ic2_120.compat.has(condensator)
            )
            .save(exporter, Identifier.fromNamespaceAndPath("ic2_120", "${condensator.toString().replace(':','_')}_repair_with_${repairItem.toString().replace(':','_')}"))
    }
}

/**
 * 冷凝器：可存储热量但不主动散热，耐久减少不可逆，只能在工作台修复。
 */
abstract class CondensatorItem(
    settings: Item.Properties,
    maxHeat: Int
) : AbstractDamageableReactorComponent(settings, maxHeat) {

    override fun canStoreHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Boolean = true

    override fun getMaxHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = maxUse

    override fun getCurrentHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = getUse(stack)

    override fun alterHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int {
        // 冷凝器只接受热量增加，不允许热量减少（不可被热交换器修复）
        if (heat < 0) return heat  // 拒绝热量减少，返回原热量值

        var myHeat = getCurrentHeat(stack, reactor, x, y)
        myHeat += heat
        val max = getMaxHeat(stack, reactor, x, y)
        return if (myHeat > max) {
            reactor.setItemAt(x, y, null)
            max - myHeat + heat  // 返回未能吸收的热量（溢出）
        } else {
            setUse(stack, myHeat)
            0
        }
    }

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        // 冷凝器不主动行动，仅被动存储热量
    }

    override fun acceptUraniumPulse(
        stack: ItemStack,
        reactor: IReactor,
        pulsingStack: ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = false

    /**
     * 检查给定物品是否可修复此冷凝器
     */
    abstract fun canRepairWith(item: Item): Boolean

    /**
     * 获取修复物品可恢复的热量容量
     */
    abstract fun getRepairAmount(item: Item): Int

    fun getRecipeRemainder(stack: ItemStack): ItemStack {
        // 配方系统会调用此方法获取剩余物。
        // 修复配方语义：不消耗冷凝器本体，只消耗修复材料（红石/青金石），
        // 产出全新（满容量）冷凝器，原物品随格子保留。
        // 注意必须 copyWithCount(1)：组件可堆叠后，若返回整个堆叠的 copy，
        // CraftingResultSlot 会把剩余物与格子内剩余数量合并，导致每合成一次
        // 数量越滚越大（64 → 127 → ...）的物品复制漏洞。
        return stack.copyWithCount(1)
    }
}

/**
 * 反应堆冷却单元：可存储热量，不主动与反应堆交换热量，但可与其他组件交换。
 * 作为被动热缓冲，供燃料棒优先传热。
 */
abstract class ReactorCoolantCellBase(
    settings: Item.Properties,
    maxHeat: Int
) : AbstractDamageableReactorComponent(settings, maxHeat) {

    override fun canStoreHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Boolean = true

    override fun getMaxHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = maxUse

    override fun getCurrentHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = getUse(stack)

    override fun alterHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int {
        // 允许与其他组件双向热交换，像散热片一样
        var myHeat = getCurrentHeat(stack, reactor, x, y)
        myHeat += heat
        val max = getMaxHeat(stack, reactor, x, y)
        return if (myHeat > max) {
            reactor.setItemAt(x, y, null)
            max - myHeat + heat  // 返回未能吸收的热量（溢出）
        } else {
            if (myHeat < 0) {
                val overflow = myHeat
                myHeat = 0
                setUse(stack, 0)
                overflow
            } else {
                setUse(stack, myHeat)
                0
            }
        }
    }

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        // 冷却单元不主动行动：不吸堆温、不散热
        // 但可以被其他组件（热交换器、元件散热片）通过 alterHeat 交换热量
    }

    override fun acceptUraniumPulse(
        stack: ItemStack,
        reactor: IReactor,
        pulsingStack: ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = false
}

@ModItem(name = "reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorCoolantCellItem : ReactorCoolantCellBase(Item.Properties(), 10_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 十字形配方：中间冷却液单元，上下左右4个锡板
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ReactorCoolantCellItem::class.instance(), 1)
                .pattern(" T ")
                .pattern("TCT")
                .pattern(" T ")
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_TIN))
                .define('C', CoolantCell::class.instance())
                .unlockedBy(net.minecraft.data.recipes.RecipeProvider.getHasName(TinPlate::class.instance()), ic2_120.compat.has(TinPlate::class.instance()))
                .save(exporter)
        }
    }
}

@ModItem(name = "triple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class TripleReactorCoolantCellItem : ReactorCoolantCellBase(Item.Properties(), 30_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 中间行3个10k，上下两行6个锡板
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, TripleReactorCoolantCellItem::class.instance(), 1)
                .pattern("TTT")
                .pattern("CCC")
                .pattern("TTT")
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_TIN))
                .define('C', ReactorCoolantCellItem::class.instance())
                .unlockedBy(net.minecraft.data.recipes.RecipeProvider.getHasName(ReactorCoolantCellItem::class.instance()), ic2_120.compat.has(ReactorCoolantCellItem::class.instance()))
                .save(exporter)
        }
    }
}

@ModItem(name = "sextuple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class SextupleReactorCoolantCellItem : ReactorCoolantCellBase(Item.Properties(), 60_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 上下两行6个锡板，中间行2个30k + 1个铁板
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SextupleReactorCoolantCellItem::class.instance(), 1)
                .pattern("TTT")
                .pattern("SIS")
                .pattern("TTT")
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_TIN))
                .define('S', TripleReactorCoolantCellItem::class.instance())
                .define('I', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_IRON))
                .unlockedBy(net.minecraft.data.recipes.RecipeProvider.getHasName(TripleReactorCoolantCellItem::class.instance()), ic2_120.compat.has(TripleReactorCoolantCellItem::class.instance()))
                .save(exporter)
        }
    }
}

@ModItem(name = "reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorPlatingItem : AbstractReactorComponent(Item.Properties()) {
    companion object {
        const val HEAT_BONUS = 500
        const val EXPLOSION_MODIFIER = 0.9f  // -10% 爆炸范围（累乘）

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 1个铅板 + 1个高基合金（竖放）
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ReactorPlatingItem::class.instance(), 1)
                .pattern("L")
                .pattern("A")
                .define('L', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_LEAD))
                .define('A', Alloy::class.instance())
                .unlockedBy(net.minecraft.data.recipes.RecipeProvider.getHasName(LeadPlate::class.instance()), ic2_120.compat.has(LeadPlate::class.instance()))
                .save(exporter)
        }
    }

    override fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float = EXPLOSION_MODIFIER

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (heatRun) {
            reactor.setHeatEffectModifier(reactor.getHeatEffectModifier() * EXPLOSION_MODIFIER)
        }
    }
}

@ModItem(name = "reactor_heat_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatPlatingItem : AbstractReactorComponent(Item.Properties()) {
    companion object {
        const val HEAT_BONUS = 1700
        const val EXPLOSION_MODIFIER = 0.99f  // -1% 爆炸范围（累乘）

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 1个普通隔板（中间） + 8个铜板
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ReactorHeatPlatingItem::class.instance(), 1)
                .pattern("CCC")
                .pattern("CPC")
                .pattern("CCC")
                .define('C', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_COPPER))
                .define('P', ReactorPlatingItem::class.instance())
                .unlockedBy(net.minecraft.data.recipes.RecipeProvider.getHasName(ReactorPlatingItem::class.instance()), ic2_120.compat.has(ReactorPlatingItem::class.instance()))
                .save(exporter)
        }
    }

    override fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float = EXPLOSION_MODIFIER

    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (heatRun) {
            reactor.setHeatEffectModifier(reactor.getHeatEffectModifier() * EXPLOSION_MODIFIER)
        }
    }
}

@ModItem(name = "containment_reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ContainmentReactorPlatingItem : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val plating = ReactorPlatingItem::class.instance()
            val alloy = Alloy::class.instance()
            if (plating == Items.AIR || alloy == Items.AIR) return
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ContainmentReactorPlatingItem::class.instance(), 1)
                .pattern("   ")
                .pattern("   ")
                .pattern("PAA")
                .define('P', plating)
                .define('A', alloy)
                .unlockedBy(
                    net.minecraft.data.recipes.RecipeProvider.getHasName(plating),
                    ic2_120.compat.has(plating)
                )
                .save(exporter)
        }
    }
}

@ModItem(name = "neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class NeutronReflectorItem : AbstractFiniteNeutronReflectorItem(Item.Properties(), 30_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // TCT / CPC / TCT — 锡粉、煤粉、铜板
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, NeutronReflectorItem::class.instance(), 1)
                .pattern("TCT")
                .pattern("CPC")
                .pattern("TCT")
                .define('T', ic2_120.compat.tagIngredient(ModTags.Compat.Items.DUSTS_TIN))
                .define('C', CoalDust::class.instance())
                .define('P', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_COPPER))
                .unlockedBy(
                    net.minecraft.data.recipes.RecipeProvider.getHasName(CopperPlate::class.instance()),
                    ic2_120.compat.has(CopperPlate::class.instance())
                )
                .save(exporter)
        }
    }
}

@ModItem(name = "thick_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ThickNeutronReflectorItem : AbstractFiniteNeutronReflectorItem(Item.Properties(), 120_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 四角+中心铜板，四边中子反射板
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ThickNeutronReflectorItem::class.instance(), 1)
                .pattern("CRC")
                .pattern("RCR")
                .pattern("CRC")
                .define('C', ic2_120.compat.tagIngredient(ModTags.Compat.Items.PLATES_COPPER))
                .define('R', NeutronReflectorItem::class.instance())
                .unlockedBy(
                    net.minecraft.data.recipes.RecipeProvider.getHasName(NeutronReflectorItem::class.instance()),
                    ic2_120.compat.has(NeutronReflectorItem::class.instance())
                )
                .save(exporter)
        }
    }
}

@ModItem(name = "iridium_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class IridiumNeutronReflectorItem : AbstractReactorComponent(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val thick = ThickNeutronReflectorItem::class.instance()
            val denseCu = DenseCopperPlate::class.instance()
            val iridium = IridiumPlate::class.instance()
            val hasThick = net.minecraft.data.recipes.RecipeProvider.getHasName(thick)
            val unlockThick = ic2_120.compat.has(thick)

            // RRR / DID / RRR
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, IridiumNeutronReflectorItem::class.instance(), 1)
                .pattern("RRR")
                .pattern("DID")
                .pattern("RRR")
                .define('R', thick)
                .define('D', denseCu)
                .define('I', iridium)
                .unlockedBy(hasThick, unlockThick)
                .save(exporter, IridiumNeutronReflectorItem::class.recipeId("from_bands_horizontal"))

            // RDR / RIR / RDR
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, IridiumNeutronReflectorItem::class.instance(), 1)
                .pattern("RDR")
                .pattern("RIR")
                .pattern("RDR")
                .define('R', thick)
                .define('D', denseCu)
                .define('I', iridium)
                .unlockedBy(hasThick, unlockThick)
                .save(exporter, IridiumNeutronReflectorItem::class.recipeId("from_bands_vertical"))
        }
    }

    override fun acceptUraniumPulse(
        stack: net.minecraft.world.item.ItemStack,
        reactor: ic2_120.content.reactor.IReactor,
        pulsingStack: net.minecraft.world.item.ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = true
}

@ModItem(name = "rsh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class RshCondensatorItem : CondensatorItem(Item.Properties(), 20_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 散热片中间，红石围7个，下面中间空着放热交换器
            // [R R R]
            // [R V R]
            // [R H R]
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, RshCondensatorItem::class.instance(), 1)
                .pattern("RRR")
                .pattern("RVR")
                .pattern("RHR")
                .define('R', Items.REDSTONE)
                .define('V', HeatVentItem::class.instance())
                .define('H', HeatExchangerItem::class.instance())
                .unlockedBy(net.minecraft.data.recipes.RecipeProvider.getHasName(Items.REDSTONE), ic2_120.compat.has(Items.REDSTONE))
                .save(exporter)

            // 修复配方：红石冷凝器 + 红石 = 修复的冷凝器
            CondensatorRepairRecipe.generate(exporter, RshCondensatorItem::class.instance(), Items.REDSTONE, 10_000)
        }
    }

    override fun canRepairWith(item: Item): Boolean = item === Items.REDSTONE
    override fun getRepairAmount(item: Item): Int = if (item === Items.REDSTONE) 10_000 else 0
}

@ModItem(name = "lzh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class LzhCondensatorItem : CondensatorItem(Item.Properties(), 100_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 中间青金石块，第一行中间散热片，四角红石，左右红石冷凝模块，底下反应堆热交换器
            // [R V R]
            // [L C L]
            // [R H R]
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, LzhCondensatorItem::class.instance(), 1)
                .pattern("RVR")
                .pattern("LCL")
                .pattern("RHR")
                .define('R', Items.REDSTONE)
                .define('V', HeatVentItem::class.instance())
                .define('L', RshCondensatorItem::class.instance())
                .define('C', Items.LAPIS_BLOCK)
                .define('H', ReactorHeatExchangerItem::class.instance())
                .unlockedBy(net.minecraft.data.recipes.RecipeProvider.getHasName(RshCondensatorItem::class.instance()), ic2_120.compat.has(RshCondensatorItem::class.instance()))
                .save(exporter)

            // 修复配方：青金石冷凝器 + 红石
            CondensatorRepairRecipe.generate(exporter, LzhCondensatorItem::class.instance(), Items.REDSTONE, 10_000)
            // 修复配方：青金石冷凝器 + 青金石
            CondensatorRepairRecipe.generate(exporter, LzhCondensatorItem::class.instance(), Items.LAPIS_LAZULI, 40_000)
        }
    }

    override fun canRepairWith(item: Item): Boolean =
        item === Items.REDSTONE || item === Items.LAPIS_LAZULI

    override fun getRepairAmount(item: Item): Int = when (item) {
        Items.LAPIS_LAZULI -> 40_000
        Items.REDSTONE -> 10_000
        else -> 0
    }
}

@ModItem(name = "lithium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class LithiumFuelRodItem : Item(Item.Properties())

@ModItem(name = "depleted_isotope_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedIsotopeFuelRodItem : Item(Item.Properties())

//已删除
// @ModItem(name = "heatpack", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatpackItem : Item(Item.Properties())

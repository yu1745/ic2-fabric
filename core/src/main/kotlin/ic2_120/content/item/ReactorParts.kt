package ic2_120.content.item

import ic2_120.content.reactor.AbstractDamageableReactorComponent
import ic2_120.content.reactor.AbstractFiniteNeutronReflectorItem
import ic2_120.content.reactor.AbstractReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier

// ========== 反应堆核心部件 ==========

/**
 * 冷凝器修复配方生成器
 */
private object CondensatorRepairRecipe {
    fun generate(exporter: RecipeExporter, condensator: Item, repairItem: Item, repairAmount: Int) {
        // 使用特殊配方类型，这里暂时用 ShapelessRecipeJsonBuilder
        // 注意：这只是占位，实际修复逻辑需要在 CondensatorItem 中实现
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, condensator, 1)
            .input(condensator)
            .input(repairItem)
            .criterion(
                FabricRecipeProvider.hasItem(condensator),
                FabricRecipeProvider.conditionsFromItem(condensator)
            )
            .offerTo(exporter, Identifier.of("ic2_120", "${condensator.toString().replace(':','_')}_repair_with_${repairItem.toString().replace(':','_')}"))
    }
}

/**
 * 冷凝器：可存储热量但不主动散热，耐久减少不可逆，只能在工作台修复。
 */
abstract class CondensatorItem(
    settings: Item.Settings,
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

    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        // 配方系统会调用此方法获取剩余物
        // 但我们使用特殊配方来处理修复，所以这里返回原物品
        return stack.copy()
    }
}

/**
 * 反应堆冷却单元：可存储热量，不主动与反应堆交换热量，但可与其他组件交换。
 * 作为被动热缓冲，供燃料棒优先传热。
 */
abstract class ReactorCoolantCellBase(
    settings: Item.Settings,
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
class ReactorCoolantCellItem : ReactorCoolantCellBase(Item.Settings(), 10_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 十字形配方：中间冷却液单元，上下左右4个锡板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorCoolantCellItem::class.instance(), 1)
                .pattern(" T ")
                .pattern("TCT")
                .pattern(" T ")
                .input('T', TinPlate::class.instance())
                .input('C', CoolantCell::class.instance())
                .criterion(FabricRecipeProvider.hasItem(TinPlate::class.instance()), FabricRecipeProvider.conditionsFromItem(TinPlate::class.instance()))
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "triple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class TripleReactorCoolantCellItem : ReactorCoolantCellBase(Item.Settings(), 30_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 中间行3个10k，上下两行6个锡板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, TripleReactorCoolantCellItem::class.instance(), 1)
                .pattern("TTT")
                .pattern("CCC")
                .pattern("TTT")
                .input('T', TinPlate::class.instance())
                .input('C', ReactorCoolantCellItem::class.instance())
                .criterion(FabricRecipeProvider.hasItem(ReactorCoolantCellItem::class.instance()), FabricRecipeProvider.conditionsFromItem(ReactorCoolantCellItem::class.instance()))
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "sextuple_reactor_coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class SextupleReactorCoolantCellItem : ReactorCoolantCellBase(Item.Settings(), 60_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 上下两行6个锡板，中间行2个30k + 1个铁板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SextupleReactorCoolantCellItem::class.instance(), 1)
                .pattern("TTT")
                .pattern("SIS")
                .pattern("TTT")
                .input('T', TinPlate::class.instance())
                .input('S', TripleReactorCoolantCellItem::class.instance())
                .input('I', IronPlate::class.instance())
                .criterion(FabricRecipeProvider.hasItem(TripleReactorCoolantCellItem::class.instance()), FabricRecipeProvider.conditionsFromItem(TripleReactorCoolantCellItem::class.instance()))
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorPlatingItem : AbstractReactorComponent(Item.Settings()) {
    companion object {
        const val HEAT_BONUS = 500
        const val EXPLOSION_MODIFIER = 0.9f  // -10% 爆炸范围（累乘）

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 1个铅板 + 1个高基合金（竖放）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorPlatingItem::class.instance(), 1)
                .pattern("L")
                .pattern("A")
                .input('L', LeadPlate::class.instance())
                .input('A', Alloy::class.instance())
                .criterion(FabricRecipeProvider.hasItem(LeadPlate::class.instance()), FabricRecipeProvider.conditionsFromItem(LeadPlate::class.instance()))
                .offerTo(exporter)
        }
    }

    override fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float = EXPLOSION_MODIFIER
}

@ModItem(name = "reactor_heat_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatPlatingItem : AbstractReactorComponent(Item.Settings()) {
    companion object {
        const val HEAT_BONUS = 1700
        const val EXPLOSION_MODIFIER = 0.99f  // -1% 爆炸范围（累乘）

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 1个普通隔板（中间） + 8个铜板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorHeatPlatingItem::class.instance(), 1)
                .pattern("CCC")
                .pattern("CPC")
                .pattern("CCC")
                .input('C', CopperPlate::class.instance())
                .input('P', ReactorPlatingItem::class.instance())
                .criterion(FabricRecipeProvider.hasItem(ReactorPlatingItem::class.instance()), FabricRecipeProvider.conditionsFromItem(ReactorPlatingItem::class.instance()))
                .offerTo(exporter)
        }
    }

    override fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float = EXPLOSION_MODIFIER
}

@ModItem(name = "containment_reactor_plating", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ContainmentReactorPlatingItem : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val plating = ReactorPlatingItem::class.instance()
            val alloy = Alloy::class.instance()
            if (plating == Items.AIR || alloy == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ContainmentReactorPlatingItem::class.instance(), 1)
                .pattern("   ")
                .pattern("   ")
                .pattern("PAA")
                .input('P', plating)
                .input('A', alloy)
                .criterion(
                    FabricRecipeProvider.hasItem(plating),
                    FabricRecipeProvider.conditionsFromItem(plating)
                )
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class NeutronReflectorItem : AbstractFiniteNeutronReflectorItem(Item.Settings(), 30_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // TCT / CPC / TCT — 锡粉、煤粉、铜板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, NeutronReflectorItem::class.instance(), 1)
                .pattern("TCT")
                .pattern("CPC")
                .pattern("TCT")
                .input('T', TinDust::class.instance())
                .input('C', CoalDust::class.instance())
                .input('P', CopperPlate::class.instance())
                .criterion(
                    FabricRecipeProvider.hasItem(CopperPlate::class.instance()),
                    FabricRecipeProvider.conditionsFromItem(CopperPlate::class.instance())
                )
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "thick_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ThickNeutronReflectorItem : AbstractFiniteNeutronReflectorItem(Item.Settings(), 120_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 四角+中心铜板，四边中子反射板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ThickNeutronReflectorItem::class.instance(), 1)
                .pattern("CRC")
                .pattern("RCR")
                .pattern("CRC")
                .input('C', CopperPlate::class.instance())
                .input('R', NeutronReflectorItem::class.instance())
                .criterion(
                    FabricRecipeProvider.hasItem(NeutronReflectorItem::class.instance()),
                    FabricRecipeProvider.conditionsFromItem(NeutronReflectorItem::class.instance())
                )
                .offerTo(exporter)
        }
    }
}

@ModItem(name = "iridium_neutron_reflector", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class IridiumNeutronReflectorItem : AbstractReactorComponent(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val thick = ThickNeutronReflectorItem::class.instance()
            val denseCu = DenseCopperPlate::class.instance()
            val iridium = IridiumPlate::class.instance()
            val hasThick = FabricRecipeProvider.hasItem(thick)
            val unlockThick = FabricRecipeProvider.conditionsFromItem(thick)

            // RRR / DID / RRR
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumNeutronReflectorItem::class.instance(), 1)
                .pattern("RRR")
                .pattern("DID")
                .pattern("RRR")
                .input('R', thick)
                .input('D', denseCu)
                .input('I', iridium)
                .criterion(hasThick, unlockThick)
                .offerTo(exporter, IridiumNeutronReflectorItem::class.recipeId("from_bands_horizontal"))

            // RDR / RIR / RDR
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumNeutronReflectorItem::class.instance(), 1)
                .pattern("RDR")
                .pattern("RIR")
                .pattern("RDR")
                .input('R', thick)
                .input('D', denseCu)
                .input('I', iridium)
                .criterion(hasThick, unlockThick)
                .offerTo(exporter, IridiumNeutronReflectorItem::class.recipeId("from_bands_vertical"))
        }
    }

    override fun acceptUraniumPulse(
        stack: net.minecraft.item.ItemStack,
        reactor: ic2_120.content.reactor.IReactor,
        pulsingStack: net.minecraft.item.ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean = true
}

@ModItem(name = "rsh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class RshCondensatorItem : CondensatorItem(Item.Settings(), 20_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 散热片中间，红石围7个，下面中间空着放热交换器
            // [R R R]
            // [R V R]
            // [R H R]
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RshCondensatorItem::class.instance(), 1)
                .pattern("RRR")
                .pattern("RVR")
                .pattern("RHR")
                .input('R', Items.REDSTONE)
                .input('V', HeatVentItem::class.instance())
                .input('H', HeatExchangerItem::class.instance())
                .criterion(FabricRecipeProvider.hasItem(Items.REDSTONE), FabricRecipeProvider.conditionsFromItem(Items.REDSTONE))
                .offerTo(exporter)

            // 修复配方：红石冷凝器 + 红石 = 修复的冷凝器
            CondensatorRepairRecipe.generate(exporter, RshCondensatorItem::class.instance(), Items.REDSTONE, 10_000)
        }
    }

    override fun canRepairWith(item: Item): Boolean = item === Items.REDSTONE
    override fun getRepairAmount(item: Item): Int = if (item === Items.REDSTONE) 10_000 else 0
}

@ModItem(name = "lzh_condensator", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class LzhCondensatorItem : CondensatorItem(Item.Settings(), 100_000) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 中间青金石块，第一行中间散热片，四角红石，左右红石冷凝模块，底下反应堆热交换器
            // [R V R]
            // [L C L]
            // [R H R]
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LzhCondensatorItem::class.instance(), 1)
                .pattern("RVR")
                .pattern("LCL")
                .pattern("RHR")
                .input('R', Items.REDSTONE)
                .input('V', HeatVentItem::class.instance())
                .input('L', RshCondensatorItem::class.instance())
                .input('C', Items.LAPIS_BLOCK)
                .input('H', ReactorHeatExchangerItem::class.instance())
                .criterion(FabricRecipeProvider.hasItem(RshCondensatorItem::class.instance()), FabricRecipeProvider.conditionsFromItem(RshCondensatorItem::class.instance()))
                .offerTo(exporter)

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
class LithiumFuelRodItem : Item(Item.Settings())

@ModItem(name = "depleted_isotope_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedIsotopeFuelRodItem : Item(Item.Settings())

//已删除
// @ModItem(name = "heatpack", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatpackItem : Item(Item.Settings())

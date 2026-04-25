package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.content.reactor.AbstractDamageableReactorComponent
import ic2_120.content.reactor.AbstractReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.content.reactor.IReactorComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.ItemStack
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

// 发热计算
private fun triangularNumber(x: Int): Int = (x * x + x) * 2

private fun checkPulseable(
    reactor: IReactor,
    x: Int,
    y: Int,
    stack: ItemStack,
    mex: Int,
    mey: Int,
    heatRun: Boolean
): Int {
    val other = reactor.getItemAt(x, y) ?: return 0
    return if (other.item is IReactorComponent && (other.item as IReactorComponent).acceptUraniumPulse(
            other,
            reactor,
            stack,
            x,
            y,
            mex,
            mey,
            heatRun
        )
    ) 1 else 0
}

private fun checkHeatAcceptor(reactor: IReactor, x: Int, y: Int, out: MutableList<Triple<ItemStack, Int, Int>>) {
    val stack = reactor.getItemAt(x, y) ?: return
    if (stack.item is IReactorComponent && (stack.item as IReactorComponent).canStoreHeat(stack, reactor, x, y)) {
        out.add(Triple(stack, x, y))
    }
}

abstract class AbstractUraniumFuelRodItem(settings: FabricItemSettings, maxUse: Int, val numberOfCells: Int) :
    AbstractDamageableReactorComponent(settings, maxUse) {
    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (!reactor.produceEnergy()) return

        val basePulses = 1 + numberOfCells / 2
        val neighborPulses = checkPulseable(reactor, x - 1, y, stack, x, y, heatRun) +
                checkPulseable(reactor, x + 1, y, stack, x, y, heatRun) +
                checkPulseable(reactor, x, y - 1, stack, x, y, heatRun) +
                checkPulseable(reactor, x, y + 1, stack, x, y, heatRun)

        if (!heatRun) {
            // 一次性计算总发电量（热模式下不发电）
            if (!reactor.isFluidCooled()) {
                val totalPulses = (basePulses + neighborPulses) * numberOfCells
                reactor.addOutput(totalPulses.toFloat())
            }
        } else {
            // 一次性计算总热量
            val totalPulses = (basePulses + neighborPulses)
            var heat = triangularNumber(basePulses + neighborPulses) * numberOfCells
            // 报告总产热
            reactor.addHeatProduced(heat)
            // 报告槽位产热和发电（每个脉冲产生1单位输出）
            val energyOutput = totalPulses.toFloat() * numberOfCells
            reactor.addSlotHeatInfo(x * 9 + y, heat, 0, energyOutput)
            // println("heat: $heat")
            val heatAcceptors = mutableListOf<Triple<ItemStack, Int, Int>>()
            checkHeatAcceptor(reactor, x - 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x + 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x, y - 1, heatAcceptors)
            checkHeatAcceptor(reactor, x, y + 1, heatAcceptors)

            // 热量分配循环：将燃料棒产生的热量优先传给邻接可储热组件
            // 算法：每轮将剩余热量平均分配给所有待处理组件，组件无法吸收的溢出热量返回池中继续分配
            while (heatAcceptors.isNotEmpty() && heat > 0) {
                // 计算每个组件本轮应分得的热量（平均分配）
                val dheat = heat / heatAcceptors.size
                heat -= dheat
                // 取出第一个待处理的组件
                val (acceptorStack, ax, ay) = heatAcceptors.removeAt(0)
                val comp = acceptorStack.item as IReactorComponent
                // 尝试向组件传递热量，返回无法吸收的溢出热量
                // 例如：散热片热容量已满时，alterHeat 返回传入的热量作为 overflow
                val overflow = comp.alterHeat(acceptorStack, reactor, ax, ay, dheat)
                // 溢出热量重新加入待分配池，继续分配给剩余组件
                heat += overflow
            }
            // 四周器件无法吸收全部热量，把热量加到堆温
            if (heat > 0) reactor.addHeat(heat)
        }

        if (!heatRun) {
            if (getUse(stack) >= maxUse - 1) {
                reactor.setItemAt(x, y, getDepletedStack())
            } else {
                incrementUse(stack)
            }
        }
    }

    abstract fun getDepletedStack(): ItemStack

    override fun acceptUraniumPulse(
        stack: ItemStack,
        reactor: IReactor,
        pulsingStack: ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean {
        // 燃料棒枯竭后不再接受铀脉冲
        return getUse(stack) < maxUse - 1
    }

    override fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float = (2 * numberOfCells).toFloat()
}

@ModItem(name = "uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class UraniumFuelRodItem : AbstractUraniumFuelRodItem(Item.Settings(), 20_000, 1) {
    override fun getDepletedStack(): ItemStack =
        ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "depleted_uranium_fuel_rod")))
}

@ModItem(name = "dual_uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DualUraniumFuelRodItem : AbstractUraniumFuelRodItem(Item.Settings(), 20_000, 2) {
    override fun getDepletedStack(): ItemStack =
        ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "depleted_dual_uranium_fuel_rod")))

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 双联 <- 单联 × 2：XFX（X=单联燃料棒, F=铁板）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, DualUraniumFuelRodItem::class.instance(), 1)
                .pattern("XFX")
                .input('X', UraniumFuelRodItem::class.instance())
                .input('F', IronPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, DualUraniumFuelRodItem::class.id())
        }
    }
}

@ModItem(name = "quad_uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class QuadUraniumFuelRodItem : AbstractUraniumFuelRodItem(Item.Settings(), 20_000, 4) {
    override fun getDepletedStack(): ItemStack =
        ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "depleted_quad_uranium_fuel_rod")))

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 四联 <- 双联：OXO / CFC / OXO（X=双联, F=铁板, C=铜板, O=空气）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuadUraniumFuelRodItem::class.instance(), 1)
                .pattern(" X ").pattern("CFC").pattern(" X ")
                .input('X', DualUraniumFuelRodItem::class.instance())
                .input('F', IronPlate::class.instance())
                .input('C', CopperPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, QuadUraniumFuelRodItem::class.id())

            // 四联 <- 单联 × 2：XFX / CFC / XFX（X=单联）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuadUraniumFuelRodItem::class.instance(), 1)
                .pattern("XFX").pattern("CFC").pattern("XFX")
                .input('X', UraniumFuelRodItem::class.instance())
                .input('F', IronPlate::class.instance())
                .input('C', CopperPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "quad_uranium_fuel_rod_from_single"))
        }
    }
}

// ========== 枯竭燃料棒（占位，不可发电） ==========

@ModItem(name = "depleted_uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedUraniumFuelRodItem : AbstractReactorComponent(Item.Settings())

@ModItem(name = "depleted_dual_uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedDualUraniumFuelRodItem : AbstractReactorComponent(Item.Settings())

@ModItem(name = "depleted_quad_uranium_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedQuadUraniumFuelRodItem : AbstractReactorComponent(Item.Settings())

abstract class AbstractMoxFuelRodItem(settings: FabricItemSettings, maxUse: Int, val numberOfCells: Int) :
    AbstractDamageableReactorComponent(settings, maxUse) {
    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (!reactor.produceEnergy()) return

        val basePulses = 1 + numberOfCells / 2
        val neighborPulses = checkPulseable(reactor, x - 1, y, stack, x, y, heatRun) +
                checkPulseable(reactor, x + 1, y, stack, x, y, heatRun) +
                checkPulseable(reactor, x, y - 1, stack, x, y, heatRun) +
                checkPulseable(reactor, x, y + 1, stack, x, y, heatRun)

        if (!heatRun) {
            // 一次性计算总发电量（热模式下不发电）
            if (!reactor.isFluidCooled()) {
                val totalPulses = (basePulses + neighborPulses) * numberOfCells
                // for (p in 0 until totalPulses) {
                //     acceptUraniumPulse(stack, reactor, stack, x, y, x, y, heatRun)
                // }
                val breedereffectiveness = reactor.getHeat().toFloat() / reactor.getMaxHeat().toFloat()
                val reaktorOutput = 4.0f * breedereffectiveness + 1.0f
                //     reactor.addOutput(reaktorOutput)
                reactor.addOutput(totalPulses.toFloat() * reaktorOutput)
            }
        } else {
            // 一次性计算总热量
            val totalPulses = (basePulses + neighborPulses)
            val rawHeat = triangularNumber(totalPulses) * numberOfCells
            val finalHeat = getFinalHeat(stack, reactor, x, y, rawHeat)
            var heat = finalHeat
            // 报告总产热
            reactor.addHeatProduced(heat)
            // 报告槽位产热和发电（每个脉冲产生1单位输出）
            val energyOutput = (totalPulses * numberOfCells).toFloat()
            reactor.addSlotHeatInfo(x * 9 + y, heat, 0, energyOutput)
            val heatAcceptors = mutableListOf<Triple<ItemStack, Int, Int>>()
            checkHeatAcceptor(reactor, x - 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x + 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x, y - 1, heatAcceptors)
            checkHeatAcceptor(reactor, x, y + 1, heatAcceptors)

            while (heatAcceptors.isNotEmpty() && heat > 0) {
                val dheat = heat / heatAcceptors.size
                heat -= dheat
                val (acceptorStack, ax, ay) = heatAcceptors.removeAt(0)
                val comp = acceptorStack.item as IReactorComponent
                val overflow = comp.alterHeat(acceptorStack, reactor, ax, ay, dheat)
                heat += overflow
            }
            if (heat > 0) reactor.addHeat(heat)

        }

        if (!heatRun) {
            if (getUse(stack) >= maxUse - 1) {
                reactor.setItemAt(x, y, getDepletedStack())
            } else {
                incrementUse(stack)
            }
        }
    }

    protected fun getFinalHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int {
        if (reactor.isFluidCooled()) {
            val breedereffectiveness = reactor.getCycleStartHeat().toFloat() / reactor.getMaxHeat().toFloat()
            if (breedereffectiveness > 0.5) {
                return heat * 2
            }
        }
        return heat
    }

    abstract fun getDepletedStack(): ItemStack

    override fun acceptUraniumPulse(
        stack: ItemStack,
        reactor: IReactor,
        pulsingStack: ItemStack,
        youX: Int,
        youY: Int,
        pulseX: Int,
        pulseY: Int,
        heatRun: Boolean
    ): Boolean {
        // 燃料棒枯竭后不再接受铀脉冲
        return getUse(stack) < maxUse - 1
    }

    override fun influenceExplosion(stack: ItemStack, reactor: IReactor): Float = (2 * numberOfCells).toFloat()
}

@ModItem(name = "mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class MoxFuelRodItem : AbstractMoxFuelRodItem(Item.Settings(), 10_000, 1) {
    override fun getDepletedStack(): ItemStack =
        ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "depleted_mox_fuel_rod")))

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 双联 <- 单联 × 2：XFX（X=单联MOX燃料棒, F=铁板）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, DualMoxFuelRodItem::class.instance(), 1)
                .pattern("XFX")
                .input('X', MoxFuelRodItem::class.instance())
                .input('F', IronPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, DualMoxFuelRodItem::class.id())
        }
    }
}

@ModItem(name = "dual_mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DualMoxFuelRodItem : AbstractMoxFuelRodItem(Item.Settings(), 10_000, 2) {
    override fun getDepletedStack(): ItemStack =
        ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "depleted_dual_mox_fuel_rod")))

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // 四联 <- 双联：OXO / CFC / OXO（X=双联MOX燃料棒, F=铁板, C=铜板, O=空气）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuadMoxFuelRodItem::class.instance(), 1)
                .pattern(" X ").pattern("CFC").pattern(" X ")
                .input('X', DualMoxFuelRodItem::class.instance())
                .input('F', IronPlate::class.instance())
                .input('C', CopperPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, QuadMoxFuelRodItem::class.id())

            // 四联 <- 单联：XFX / CFC / XFX（X=单联MOX燃料棒）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuadMoxFuelRodItem::class.instance(), 1)
                .pattern("XFX").pattern("CFC").pattern("XFX")
                .input('X', MoxFuelRodItem::class.instance())
                .input('F', IronPlate::class.instance())
                .input('C', CopperPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, Identifier.of(Ic2_120.MOD_ID, "quad_mox_fuel_rod_from_single"))
        }
    }
}

@ModItem(name = "quad_mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class QuadMoxFuelRodItem : AbstractMoxFuelRodItem(Item.Settings(), 10_000, 4) {
    override fun getDepletedStack(): ItemStack =
        ItemStack(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "depleted_quad_mox_fuel_rod")))
}

// ========== 枯竭 MOX 燃料棒 ==========

@ModItem(name = "depleted_mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedMoxFuelRodItem : AbstractReactorComponent(Item.Settings())

@ModItem(name = "depleted_dual_mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedDualMoxFuelRodItem : AbstractReactorComponent(Item.Settings())

@ModItem(name = "depleted_quad_mox_fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class DepletedQuadMoxFuelRodItem : AbstractReactorComponent(Item.Settings())

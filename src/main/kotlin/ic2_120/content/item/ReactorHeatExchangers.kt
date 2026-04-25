package ic2_120.content.item

import ic2_120.content.reactor.AbstractDamageableReactorComponent
import ic2_120.content.reactor.IReactor
import ic2_120.content.reactor.IReactorComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import kotlin.math.roundToInt

/**
 * 热交换器基类：与邻接组件和/或反应堆交换热量。
 * switchSide: 每周期与每个邻接可储热组件的最大交换量
 * switchReactor: 每周期与反应堆的最大交换量
 */
abstract class ReactorHeatExchangerBase(
    settings: FabricItemSettings,
    heatStorage: Int,
    private val switchSide: Int,
    private val switchReactor: Int
) : AbstractDamageableReactorComponent(settings, heatStorage) {

    override fun canStoreHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Boolean = true

    override fun getMaxHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = maxUse

    override fun getCurrentHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int): Int = getUse(stack)

    /**
     * 处理热量变化：吸收或释放热量
     * @param heat 热量变化值（正数为吸收，负数为释放）
     * @return 无法处理的溢出热量（正数为无法吸收的热量，负数为无法释放的热量）
     */
    override fun alterHeat(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heat: Int): Int {
        var myHeat = getCurrentHeat(stack, reactor, x, y)
        myHeat += heat
        val max = getMaxHeat(stack, reactor, x, y)
        return if (myHeat > max) {
            // 热容量超限，组件损坏（消失）
            reactor.setItemAt(x, y, null)
            // 计算溢出热量：总热量 - 最大容量 = 无法吸收的热量
            max - myHeat + heat
        } else {
            if (myHeat < 0) {
                // 热量不足，无法释放请求的热量
                val overflow = myHeat
                myHeat = 0
                setUse(stack, 0)
                overflow
            } else {
                // 热量变化有效，更新存储的热量
                setUse(stack, myHeat)
                0
            }
        }
    }

    /**
     * 处理热交换逻辑：与邻接组件和/或反应堆交换热量
     * 仅在热量运行阶段（heatRun=true）执行
     */
    override fun processChamber(stack: ItemStack, reactor: IReactor, x: Int, y: Int, heatRun: Boolean) {
        if (!heatRun) return

        var myHeatDelta = 0

        // 与邻接可储热组件交换热量
        if (switchSide > 0) {
            val scaledSwitchSide = switchSide
            val heatAcceptors = mutableListOf<Triple<ItemStack, Int, Int>>()
            checkHeatAcceptor(reactor, x - 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x + 1, y, heatAcceptors)
            checkHeatAcceptor(reactor, x, y - 1, heatAcceptors)
            checkHeatAcceptor(reactor, x, y + 1, heatAcceptors)

            for ((otherStack, ox, oy) in heatAcceptors) {
                val comp = otherStack.item as IReactorComponent
                // 计算热交换器的热容量使用率（0-100%）
                val mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y)
                // 计算邻接组件的热容量使用率（0-100%）
                val othermed = comp.getCurrentHeat(otherStack, reactor, ox, oy) * 100.0 / comp.getMaxHeat(otherStack, reactor, ox, oy)
                
                // 计算热交换量：基于邻接组件的热容量和双方温度差异
                var add = (comp.getMaxHeat(otherStack, reactor, ox, oy) / 100.0 * (othermed + mymed / 2)).roundToInt()
                // 限制热交换量不超过最大交换速率
                add = add.coerceIn(-scaledSwitchSide, scaledSwitchSide)
                
                // 根据平均温度调整交换量，温度越低交换越慢
                val avgTemp = othermed + mymed / 2
                // 与Java版保持一致的判断顺序和计算逻辑：直接使用switchSide的固定比例
                if (avgTemp < 1.0) add = scaledSwitchSide / 2  // 高温：1/2最大交换速率
                if (avgTemp < 0.75) add = scaledSwitchSide / 4  // 中温：1/4最大交换速率
                if (avgTemp < 0.5) add = scaledSwitchSide / 8   // 较低温：1/8最大交换速率
                if (avgTemp < 0.25) add = 1                    // 低温：最小交换速率
                
                // 热量从高温向低温流动
                if (kotlin.math.round(othermed * 10) / 10.0 > kotlin.math.round(mymed * 10) / 10.0) add = -add
                else if (kotlin.math.round(othermed * 10) / 10.0 == kotlin.math.round(mymed * 10) / 10.0) add = 0
                
                // 记录热交换器的热量变化（失去的热量）
                myHeatDelta -= add
                // 向邻接组件传递热量，并处理实际交换的热量
                val actualExchange = comp.alterHeat(otherStack, reactor, ox, oy, add)
                // 如果实际交换的热量与请求的不同，调整热交换器的热量变化
                if (actualExchange != 0) {
                    myHeatDelta += actualExchange
                }
            }
        }

        // 与反应堆交换热量
        if (switchReactor > 0) {
            val scaledSwitchReactor = switchReactor
            // 计算热交换器的热容量使用率
            val mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y)
            // 计算反应堆的热容量使用率
            val reactorMed = reactor.getHeat() * 100.0 / reactor.getMaxHeat()
            
            // 计算热交换量：基于反应堆的热容量和双方温度差异
            var add = (reactor.getMaxHeat() / 100.0 * (reactorMed + mymed / 2)).roundToInt()
            // 限制热交换量不超过最大交换速率
            add = add.coerceIn(-scaledSwitchReactor, scaledSwitchReactor)
            
            // 根据平均温度调整交换量
            val avg = reactorMed + mymed / 2
            // 与Java版保持一致的判断顺序和计算逻辑：直接使用switchReactor的固定比例
            if (avg < 1.0) add = scaledSwitchReactor / 2  // 高温：1/2最大交换速率
            if (avg < 0.75) add = scaledSwitchReactor / 4  // 中温：1/4最大交换速率
            if (avg < 0.5) add = scaledSwitchReactor / 8   // 较低温：1/8最大交换速率
            if (avg < 0.25) add = 1                        // 低温：最小交换速率
            
            // 热量从高温向低温流动
            if (kotlin.math.round(reactorMed * 10) / 10.0 > kotlin.math.round(mymed * 10) / 10.0) add = -add
            else if (kotlin.math.round(reactorMed * 10) / 10.0 == kotlin.math.round(mymed * 10) / 10.0) add = 0
            
            // 记录热交换器的热量变化
            myHeatDelta -= add
            // 调整反应堆热量（确保在0-最大热量范围内）
            reactor.setHeat((reactor.getHeat() + add).coerceIn(0, reactor.getMaxHeat()))
        }

        // 应用热交换器自身的热量变化
        alterHeat(stack, reactor, x, y, myHeatDelta)
    }

    private fun checkHeatAcceptor(reactor: IReactor, x: Int, y: Int, out: MutableList<Triple<ItemStack, Int, Int>>) {
        val s = reactor.getItemAt(x, y) ?: return
        if (s.item is IReactorComponent && (s.item as IReactorComponent).canStoreHeat(s, reactor, x, y)) {
            out.add(Triple(s, x, y))
        }
    }
}

@ModItem(name = "heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class HeatExchangerItem : ReactorHeatExchangerBase(Item.Settings(), 2500, 12, 4) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val copper = CopperPlate::class.instance()
            val tin = TinPlate::class.instance()
            val circuit = Circuit::class.instance()
            if (copper != Items.AIR && tin != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, HeatExchangerItem::class.instance(), 1)
                    .pattern("CEC").pattern("TCT").pattern("CTC")
                    .input('C', copper).input('E', circuit).input('T', tin)
                    .criterion(hasItem(copper), conditionsFromItem(copper))
                    .offerTo(exporter, HeatExchangerItem::class.id())
            }
        }
    }
}

@ModItem(name = "reactor_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorHeatExchangerItem : ReactorHeatExchangerBase(Item.Settings(), 5000, 0, 72) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val plate = CopperPlate::class.instance()
            val base = HeatExchangerItem::class.instance()
            if (plate != Items.AIR && base != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorHeatExchangerItem::class.instance(), 1)
                    .pattern("PPP").pattern("PHP").pattern("PPP")
                    .input('P', plate).input('H', base)
                    .criterion(hasItem(base), conditionsFromItem(base))
                    .offerTo(exporter, ReactorHeatExchangerItem::class.id())
            }
        }
    }
}

@ModItem(name = "component_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ComponentHeatExchangerItem : ReactorHeatExchangerBase(Item.Settings(), 5000, 36, 0) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val gold = GoldPlate::class.instance()
            val base = HeatExchangerItem::class.instance()
            if (gold != Items.AIR && base != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ComponentHeatExchangerItem::class.instance(), 1)
                    .pattern(" G ").pattern("GHG").pattern(" G ")
                    .input('G', gold).input('H', base)
                    .criterion(hasItem(base), conditionsFromItem(base))
                    .offerTo(exporter, ComponentHeatExchangerItem::class.id())
            }
        }
    }
}

@ModItem(name = "advanced_heat_exchanger", tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class AdvancedHeatExchangerItem : ReactorHeatExchangerBase(Item.Settings(), 10000, 24, 8) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val lapis = LapisPlate::class.instance()
            val copper = CopperPlate::class.instance()
            val circuit = Circuit::class.instance()
            val base = HeatExchangerItem::class.instance()
            if (lapis != Items.AIR && copper != Items.AIR && circuit != Items.AIR && base != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedHeatExchangerItem::class.instance(), 1)
                    .pattern("LCL").pattern("HPH").pattern("LCL")
                    .input('L', lapis).input('C', circuit).input('P', copper).input('H', base)
                    .criterion(hasItem(base), conditionsFromItem(base))
                    .offerTo(exporter, AdvancedHeatExchangerItem::class.id())
            }
        }
    }
}

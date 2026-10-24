package ic2_120.client

import ic2_120.content.item.NanoSaber
import ic2_120.content.item.RecallScrollItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.Ic2_120
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

/**
 * 电池模型 Predicate 注册
 *
 * 为所有电池物品注册 "ic2:charge" predicate，使电池贴图随电量变化。
 */
object BatteryModelPredicates {

    private const val NBT_HAS_BIND = "HasBind"
    private val CHARGE_ID = ResourceLocation.fromNamespaceAndPath("ic2", "charge")
    private val NANO_SABER_ACTIVE_ID = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "nano_saber_active")
    private val SCROLL_BOUND_ID = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "bound")

    /**
     * 注册所有电池物品的模型 predicate
     */
    fun register() {
        // 遍历所有已注册的物品，找到电池物品并注册 predicate
        BuiltInRegistries.ITEM.ids.forEach { id ->
            if (id.namespace == "ic2_120") {
                val item = BuiltInRegistries.ITEM.get(id)
                if (item is IBatteryItem) {
                    registerChargePredicate(item, id)
                }
            }
        }
        registerNanoSaberPredicate()
        registerRecallScrollPredicate()
    }

    private fun registerNanoSaberPredicate() {
        val saber = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "nano_saber"))
        ItemProperties.register(
            saber,
            NANO_SABER_ACTIVE_ID
        ) { _, _, _, _ ->
            NanoSaber.getActiveData()
        }
    }

    /**
     * 为单个电池物品注册电量 predicate
     *
     * @param item 已注册的电池物品
     * @param itemId 物品ID
     */
    private fun registerChargePredicate(item: Item, itemId: ResourceLocation) {
        ItemProperties.register(
            item,
            CHARGE_ID
        ) { stack: ItemStack, _, _, _ ->
            // 计算电量比例 (0.0 - 1.0)
            val batteryItem = stack.item
            if (batteryItem is IBatteryItem) {
                val ratio = batteryItem.getChargeRatio(stack)
                ratio.toFloat()
            } else {
                0.0f
            }
        }
    }

    private fun registerRecallScrollPredicate() {
        val scroll = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "recall_scroll"))
        if (scroll !is RecallScrollItem) return
        ItemProperties.register(
            scroll,
            SCROLL_BOUND_ID
        ) { stack: ItemStack, _, _, _ ->
            if (stack.nbt?.getBoolean(NBT_HAS_BIND) == true) 1.0f else 0.0f
        }
    }
}

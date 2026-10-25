package ic2_120.content.item.armor

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.compat.orCreateNbt

import ic2_120.compat.isClient

import ic2_120.compat.world

import ic2_120.config.Ic2Config
import ic2_120.content.fluid.FluidFuelRegistry
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.ICreativeFullVariant
import ic2_120.content.item.ModArmorMaterials
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import ic2_120.compat.ArmorItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.phys.BlockHitResult
import kotlin.math.min

/**
 * 喷气背包 (Jetpack)
 *
 * 使用生物燃料作为动力的飞行装置，类似创造飞行，按空格上升、Shift下降。
 */
open class JetpackItem : ArmorItem(
    ModArmorMaterials.JETPACK_ARMOR,
    ArmorItem.Type.CHESTPLATE,
    // 1.21.1: ArmorItem 不再注入耐久，Item#isDamageable 已移除。不设置 durability 即
    // 不含 MAX_DAMAGE/DAMAGE 组件，ItemStack.isDamageableItem() 恒为 false（原语义）。
    Item.Properties().stacksTo(1)
), ICreativeFullVariant {

    companion object {
        private const val FUEL_KEY = "Fuel"
        private const val FUEL_FLUID_KEY = "FuelFluid"
        private const val FUEL_REMAINDER_KEY = "FuelRemainder"

        // 内部燃料以 droplets 存储（与 Fabric Transfer API 一致），配置项以 mB 为单位。
        // 1 mB = FluidConstants.BUCKET / 1000 droplets。

        @JvmStatic
        /** 内部燃料容量（droplets）。由配置的 mB 值换算而来。 */
        val maxFuel: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel * FluidConstants.BUCKET / 1000L

        @JvmStatic
        /** 内部燃料容量（droplets）。由配置的 mB 值换算而来。 */
        val MAX_FUEL: Long
            get() = maxFuel

        @JvmStatic
        val fuelPerTick: Double
            get() = Ic2Config.getJetpackFuelPerTick()

        /** 配置中的容量（mB），仅用于 UI 显示。 */
        @JvmStatic
        val maxFuelMb: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel

        /** 原油/杂酚油等效燃料的能量密度是生物燃油的一半。 */
        fun fuelConsumptionMultiplier(stack: ItemStack): Double =
            getFuelFluid(stack)?.let { fluid ->
                FluidFuelRegistry.getProfile(fluid)?.jetpackConsumptionMultiplier
            } ?: 1.0

        @JvmStatic
        val flightDurationSeconds: Int
            get() = Ic2Config.current.armor.jetpack.flightDurationSeconds

        @JvmStatic
        fun getFuel(stack: ItemStack): Long =
            stack.orCreateNbt.getLongOr(FUEL_KEY, 0L).coerceIn(0L, maxFuel)

        @JvmStatic
        fun setFuel(stack: ItemStack, fuel: Long) {
            stack.orCreateNbt = stack.orCreateNbt.apply { putLong(FUEL_KEY, fuel.coerceIn(0L, maxFuel)) }
        }

        fun getFuelFluid(stack: ItemStack): net.minecraft.world.level.material.Fluid? {
            val id = stack.orCreateNbt.getStringOr(FUEL_FLUID_KEY, "")
            if (!id.isNullOrBlank()) {
                val fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse(id))
                if (FluidFuelRegistry.isSupported(fluid)) return fluid
            }
            // 兼容旧版只有 Fuel 数值的背包：旧燃料就是生物燃料。
            return if (getFuel(stack) > 0L) ModFluids.BIOFUEL_STILL else null
        }

        fun setFuelFluid(stack: ItemStack, fluid: net.minecraft.world.level.material.Fluid?) {
            if (fluid == null) stack.orCreateNbt = stack.orCreateNbt.apply { remove(FUEL_FLUID_KEY) }
            else stack.orCreateNbt = stack.orCreateNbt.apply { putString(FUEL_FLUID_KEY, BuiltInRegistries.FLUID.getKey(fluid).toString()) }
        }

        /**
         * 使用余数累加器精确消耗燃料（支持小数消耗速率）
         * @return true 如果消耗成功，false 如果燃料不足
         */
        @JvmStatic
        fun consumeFuelPerTick(stack: ItemStack): Boolean {
            val fuel = getFuel(stack)
            if (fuel <= 0) return false
            val nbt = stack.orCreateNbt
            var remainder = nbt.getDoubleOr(FUEL_REMAINDER_KEY, 0.0)
            val cost = fuelPerTick * fuelConsumptionMultiplier(stack)
            remainder += cost
            val toConsume = remainder.toLong()
            if (toConsume <= 0) {
                nbt.putDouble(FUEL_REMAINDER_KEY, remainder)
                return true
            }
            if (fuel < toConsume) return false
            setFuel(stack, fuel - toConsume)
            nbt.putDouble(FUEL_REMAINDER_KEY, remainder - toConsume)
            return true
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay, consumer: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        val tooltip = ic2_120.compat.TooltipSinkList(consumer)
        super.appendHoverText(stack, context, display, consumer, type)

        val fuel = getFuel(stack)
        val ratio = if (maxFuel > 0) fuel.toDouble() / maxFuel else 0.0
        // 内部以 droplets 存储显示需要换算成 mB
        val fuelMb = fuel * 1000L / FluidConstants.BUCKET

        // 计算剩余飞行时间（秒）
        val remainingSeconds = if (fuel > 0 && maxFuel > 0) {
            fuel.toDouble() / maxFuel * flightDurationSeconds / fuelConsumptionMultiplier(stack)
        } else 0.0

        // 格式化时间
        val timeText = if (remainingSeconds >= 60) {
            val minutes = (remainingSeconds / 60).toInt()
            val seconds = (remainingSeconds % 60).toInt()
            "${minutes}分${seconds}秒"
        } else {
            "${remainingSeconds.toInt()}秒"
        }

        val fuelName = getFuelFluid(stack)?.let { BuiltInRegistries.FLUID.getKey(it).toString() } ?: "无"
        tooltip.add(Component.literal("燃料: $fuelName %,d / %,d mB (%.1f%%)".format(fuelMb, maxFuelMb, ratio * 100)))
        tooltip.add(Component.literal("剩余飞行: $timeText").withStyle(ChatFormatting.GRAY))
    }


    override fun useOn(context: UseOnContext): InteractionResult {
        val world = context.level
        val player = context.player ?: return InteractionResult.PASS
        if (world.isClient) return InteractionResult.SUCCESS
        val hit = BlockHitResult(context.clickLocation, context.clickedFace, context.clickedPos, context.isInside())
        val storage = FluidStorage.SIDED.find(world, hit.blockPos, hit.direction)
        return if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, context.hand)) {
            InteractionResult.SUCCESS
        } else InteractionResult.PASS
    }

    override fun isBarVisible(stack: ItemStack): Boolean = true

    override fun getBarWidth(stack: ItemStack): Int =
        ((getFuel(stack).toDouble() / maxFuel) * 13).toInt().coerceIn(0, 13)

    override fun getBarColor(stack: ItemStack): Int {
        val ratio = getFuel(stack).toDouble() / maxFuel
        return when {
            ratio > 0.5 -> 0x4AFF4A
            ratio > 0.2 -> 0xFFFF4A
            else -> 0xFF4A4A
        }
    }

    override fun createFullVariant(): ItemStack {
        return ItemStack(this).also {
            setFuel(it, MAX_FUEL)
            setFuelFluid(it, ModFluids.BIOFUEL_STILL)
        }
    }
}

internal class JetpackFluidStorage(
    private val ctx: ContainerItemContext
) : Storage<FluidVariant> {
    override fun supportsInsertion(): Boolean = true
    override fun supportsExtraction(): Boolean = true

    override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (ctx.itemVariant.item !is JetpackItem || !FluidFuelRegistry.isSupported(resource.fluid)) return 0L
        val stack = ctx.itemVariant.toStack(1)
        val currentFluid = JetpackItem.getFuelFluid(stack)
        if (currentFluid != null && currentFluid != resource.fluid) return 0L
        val current = JetpackItem.getFuel(stack)
        val inserted = min(maxAmount, JetpackItem.MAX_FUEL - current)
        if (inserted <= 0L) return 0L
        JetpackItem.setFuel(stack, current + inserted)
        JetpackItem.setFuelFluid(stack, resource.fluid)
        return if (ctx.exchange(ItemVariant.of(stack), 1, transaction) == 1L) inserted else 0L
    }

    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (ctx.itemVariant.item !is JetpackItem) return 0L
        val stack = ctx.itemVariant.toStack(1)
        val fluid = JetpackItem.getFuelFluid(stack) ?: return 0L
        if (!resource.isBlank && resource.fluid != fluid) return 0L
        val extracted = min(maxAmount, JetpackItem.getFuel(stack))
        if (extracted <= 0L) return 0L
        JetpackItem.setFuel(stack, JetpackItem.getFuel(stack) - extracted)
        if (JetpackItem.getFuel(stack) <= 0L) JetpackItem.setFuelFluid(stack, null)
        return if (ctx.exchange(ItemVariant.of(stack), 1, transaction) == 1L) extracted else 0L
    }

    override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
        if (ctx.itemVariant.item !is JetpackItem) return mutableListOf<StorageView<FluidVariant>>().iterator()
        val stack = ctx.itemVariant.toStack(1)
        val amount = JetpackItem.getFuel(stack)
        val fluid = JetpackItem.getFuelFluid(stack) ?: return mutableListOf<StorageView<FluidVariant>>().iterator()
        if (amount <= 0L) return mutableListOf<StorageView<FluidVariant>>().iterator()
        val view = object : StorageView<FluidVariant> {
            override fun getResource(): FluidVariant = FluidVariant.of(fluid)
            override fun getAmount(): Long = amount
            override fun getCapacity(): Long = JetpackItem.MAX_FUEL
            override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                this@JetpackFluidStorage.extract(resource, maxAmount, transaction)
            override fun isResourceBlank(): Boolean = false
        }
        return mutableListOf(view).iterator() as MutableIterator<StorageView<FluidVariant>>
    }
}

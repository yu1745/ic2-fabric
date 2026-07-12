package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.content.fluid.FluidFuelRegistry
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.ICreativeFullVariant
import ic2_120.content.item.ModArmorMaterials
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
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
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.hit.BlockHitResult
import kotlin.math.min

/**
 * 喷气背包 (Jetpack)
 *
 * 使用生物燃料作为动力的飞行装置，类似创造飞行，按空格上升、Shift下降。
 */
open class JetpackItem : ArmorItem(
    ModArmorMaterials.JETPACK_ARMOR,
    ArmorItem.Type.CHESTPLATE,
    // ArmorItem 会通过 maxDamageIfAbsent 自动注入材质耐久；-1 让 ItemStack.isDamageable() 返回 false。
    FabricItemSettings().maxCount(1).maxDamage(-1)
), ICreativeFullVariant {

    companion object {
        private const val FUEL_KEY = "Fuel"
        private const val FUEL_FLUID_KEY = "FuelFluid"
        private const val FUEL_REMAINDER_KEY = "FuelRemainder"

        @JvmStatic
        val maxFuel: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel

        @JvmStatic
        val MAX_FUEL: Long
            get() = Ic2Config.current.armor.jetpack.maxFuel

        @JvmStatic
        val fuelPerTick: Double
            get() = Ic2Config.getJetpackFuelPerTick()

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
            stack.orCreateNbt.getLong(FUEL_KEY).coerceIn(0L, maxFuel)

        @JvmStatic
        fun setFuel(stack: ItemStack, fuel: Long) {
            stack.orCreateNbt.putLong(FUEL_KEY, fuel.coerceIn(0L, maxFuel))
        }

        fun getFuelFluid(stack: ItemStack): net.minecraft.fluid.Fluid? {
            val id = stack.nbt?.getString(FUEL_FLUID_KEY)
            if (!id.isNullOrBlank()) {
                val fluid = net.minecraft.registry.Registries.FLUID.get(net.minecraft.util.Identifier(id))
                if (FluidFuelRegistry.isSupported(fluid)) return fluid
            }
            // 兼容旧版只有 Fuel 数值的背包：旧燃料就是生物燃料。
            return if (getFuel(stack) > 0L) ModFluids.BIOFUEL_STILL else null
        }

        fun setFuelFluid(stack: ItemStack, fluid: net.minecraft.fluid.Fluid?) {
            if (fluid == null) stack.orCreateNbt.remove(FUEL_FLUID_KEY)
            else stack.orCreateNbt.putString(FUEL_FLUID_KEY, net.minecraft.registry.Registries.FLUID.getId(fluid).toString())
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
            var remainder = nbt.getDouble(FUEL_REMAINDER_KEY)
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

    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)

        val fuel = getFuel(stack)
        val ratio = if (maxFuel > 0) fuel.toDouble() / maxFuel else 0.0

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

        val fuelName = getFuelFluid(stack)?.let { net.minecraft.registry.Registries.FLUID.getId(it).toString() } ?: "无"
        tooltip.add(Text.literal("燃料: $fuelName %,d / %,d mB (%.1f%%)".format(fuel, maxFuel, ratio * 100)))
        tooltip.add(Text.literal("剩余飞行: $timeText").formatted(Formatting.GRAY))
    }

    /**
     * 喷气背包使用燃料系统，不走原版耐久系统。
     * 若不禁用，受伤时会累积 Damage NBT 导致耐久条混乱。
     */
    override fun isDamageable(): Boolean = false

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val player = context.player ?: return ActionResult.PASS
        if (world.isClient) return ActionResult.SUCCESS
        val hit = BlockHitResult(context.hitPos, context.side, context.blockPos, context.hitsInsideBlock())
        val storage = FluidStorage.SIDED.find(world, hit.blockPos, hit.side)
        return if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, context.hand)) {
            ActionResult.SUCCESS
        } else ActionResult.PASS
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    override fun getItemBarStep(stack: ItemStack): Int =
        ((getFuel(stack).toDouble() / maxFuel) * 13).toInt().coerceIn(0, 13)

    override fun getItemBarColor(stack: ItemStack): Int {
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

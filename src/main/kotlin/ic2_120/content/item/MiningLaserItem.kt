package ic2_120.content.item

import ic2_120.content.entity.LaserMode
import ic2_120.content.entity.LaserProjectileEntity
import ic2_120.content.entity.ModEntities
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.Formatting
import net.minecraft.world.World

/**
 * 采矿镭射枪：电动远程采矿工具，发射弹射体破坏方块。
 *
 * Alt + 模式键 + 右键切换模式，右键发射。
 * 实现了 [IElectricTool]，使用 EU 电量系统。
 */
@ModItem(name = "mining_laser", tab = CreativeTab.IC2_TOOLS)
class MiningLaserItem : Item(
    FabricItemSettings().maxCount(1)
), IElectricTool {

    companion object {
        private const val MODE_KEY = "LaserMode"

        /** 从 ItemStack NBT 读取当前模式 */
        fun getMode(stack: ItemStack): LaserMode {
            val nbt = stack.nbt ?: return LaserMode.DEFAULT
            return try { LaserMode.valueOf(nbt.getString(MODE_KEY)) } catch (_: Exception) { LaserMode.DEFAULT }
        }

        /** 向 ItemStack NBT 写入模式 */
        fun setMode(stack: ItemStack, mode: LaserMode) {
            stack.getOrCreateNbt().putString(MODE_KEY, mode.name)
        }

        /** 循环切换到下一个模式 */
        fun cycleMode(stack: ItemStack): LaserMode {
            val current = getMode(stack)
            val next = LaserMode.cycle(current)
            setMode(stack, next)
            return next
        }

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val alloy = Alloy::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            val advCircuit = AdvancedCircuit::class.instance()
            if (alloy == Items.AIR || crystal == Items.AIR || advCircuit == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, MiningLaserItem::class.instance(), 1)
                .pattern("REE")
                .pattern("AAC")
                .pattern(" AA")
                .input('R', Items.REDSTONE)
                .input('E', crystal)
                .input('A', alloy)
                .input('C', advCircuit)
                .criterion(hasItem(advCircuit), conditionsFromItem(advCircuit))
                .offerTo(exporter, MiningLaserItem::class.id())
        }
    }

    // ========== IElectricTool 实现 ==========

    override val tier = 3
    override val maxCapacity = 200_000L

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun isDamageable() = false

    // ========== 使用逻辑 ==========

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        val mode = getMode(stack)

        // 电量不足
        if (getEnergy(stack) < mode.energyCost) return TypedActionResult.pass(stack)

        if (!world.isClient) {
            val pitch = user.pitch
            val yaw = user.yaw

            if (mode.scatterCount > 1) {
                // 散射/3x3 模式：发射多个弹体
                val basePitch = pitch.toDouble()
                val baseYaw = yaw.toDouble()
                val spread = mode.scatterSpread
                val count = mode.scatterCount
                val cols = kotlin.math.sqrt(count.toDouble()).toInt()
                val rows = (count + cols - 1) / cols

                var idx = 0
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        if (idx >= count) break
                        val dPitch = (row - (rows - 1) / 2.0) * spread / rows
                        val dYaw = (col - (cols - 1) / 2.0) * spread / cols
                        spawnProjectile(world, user, (basePitch + dPitch).toFloat(), (baseYaw + dYaw).toFloat(), mode)
                        idx++
                    }
                }
            } else {
                // 单发模式
                spawnProjectile(world, user, pitch, yaw, mode)
            }

            // 扣除电量
            setEnergy(stack, getEnergy(stack) - mode.energyCost)
        }

        return TypedActionResult.success(stack, true)
    }

    private fun spawnProjectile(
        world: World,
        owner: PlayerEntity,
        pitch: Float,
        yaw: Float,
        laserMode: LaserMode
    ) {
        val projectile = LaserProjectileEntity(ModEntities.LASER_PROJECTILE, world)
        projectile.init(owner, pitch, yaw, laserMode)
        world.spawnEntity(projectile)
    }

    // ========== Tooltip ==========

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)

    override fun postMine(
        stack: ItemStack,
        world: World,
        state: net.minecraft.block.BlockState,
        pos: net.minecraft.util.math.BlockPos,
        miner: LivingEntity
    ): Boolean = true
}

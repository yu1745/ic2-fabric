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
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import net.minecraft.item.ItemUsageContext
import net.minecraft.util.ActionResult
import ic2_120.getCustomData
import ic2_120.getOrCreateCustomData

/**
 * 采矿镭射枪：电动远程采矿工具，发射弹射体破坏方块。
 *
 * 按住模式键（控制里的「功能切换键」，默认 M）并右键切换模式；仅右键发射。
 * 实现了 [IElectricTool]，使用 EU 电量系统。
 */
@ModItem(name = "mining_laser", tab = CreativeTab.IC2_TOOLS)
class MiningLaserItem : Item(
    Item.Settings().maxCount(1)
), IElectricTool {

    companion object {
        private const val MODE_KEY = "LaserMode"

        /** 上一 tick 主手是否已是采矿镭射枪（用于切到主手时提示一次当前模式） */
        private val hadMiningLaserInMainLastTick = ConcurrentHashMap<UUID, Boolean>()

        /** 从 ItemStack NBT 读取当前模式 */
        fun getMode(stack: ItemStack): LaserMode {
            val nbt = stack.getCustomData() ?: return LaserMode.DEFAULT
            return try { LaserMode.valueOf(nbt.getString(MODE_KEY)) } catch (_: Exception) { LaserMode.DEFAULT }
        }

        /** 向 ItemStack NBT 写入模式 */
        fun setMode(stack: ItemStack, mode: LaserMode) {
            stack.getOrCreateCustomData().putString(MODE_KEY, mode.name)
        }

        /** 循环切换到下一个模式 */
        fun cycleMode(stack: ItemStack): LaserMode {
            val current = getMode(stack)
            val next = LaserMode.cycle(current)
            setMode(stack, next)
            return next
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
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

    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient || entity !is ServerPlayerEntity) return
        val player = entity
        val uuid = player.uuid
        if (player.mainHandStack.item !is MiningLaserItem) {
            hadMiningLaserInMainLastTick[uuid] = false
            return
        }
        if (stack !== player.mainHandStack) return

        val hadBefore = hadMiningLaserInMainLastTick.getOrDefault(uuid, false)
        if (!hadBefore) {
            val mode = getMode(stack)
            player.sendMessage(Text.translatable(mode.translationKey), true)
        }
        hadMiningLaserInMainLastTick[uuid] = true
    }

    // ========== 使用逻辑 ==========

    /**
     * 禁止与方块/实体交互，始终返回 PASS。
     * 这样游戏会跳过方块的 onUse 逻辑，直接调用 [use] 方法发射激光。
     */
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        return ActionResult.PASS
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)

        // 客户端在按住模式键切模式时仍可能发来 use；与切模式包同 tick 的这一次不发射（见 [MiningLaserServerSuppress]）
        if (!world.isClient) {
            val sp = user as? ServerPlayerEntity
            if (sp != null && MiningLaserServerSuppress.consumeSuppressNextFire(sp)) {
                return TypedActionResult.success(stack, false)
            }
        }

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

            // 设置使用冷却：5 tick（每秒最多 4 次）
            user.itemCooldownManager.set(this, 5)
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
    // 模式、按键与说明由客户端 [ic2_120.client.MiningLaserTooltipHandler] 追加

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
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

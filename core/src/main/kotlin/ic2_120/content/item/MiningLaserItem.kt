package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.config.Ic2Config
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
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.InteractionResult
import ic2_120.editCustomData
import ic2_120.getCustomData

/**
 * 采矿镭射枪：电动远程采矿工具，发射弹射体破坏方块。
 *
 * 按住模式键（控制里的「功能切换键」，默认 M）并右键切换模式；仅右键发射。
 * 实现了 [IElectricTool]，使用 EU 电量系统。
 */
@ModItem(name = "mining_laser", tab = CreativeTab.IC2_TOOLS)
class MiningLaserItem : Item(
    Item.Properties().maxStackSize(1)
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
            stack.editCustomData { it.putString(MODE_KEY, mode.name) }
        }

        /** 循环切换到下一个模式 */
        fun cycleMode(stack: ItemStack): LaserMode {
            val current = getMode(stack)
            val next = LaserMode.cycle(current)
            setMode(stack, next)
            return next
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val alloy = Alloy::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            val advCircuit = AdvancedCircuit::class.instance()
            if (alloy == Items.AIR || crystal == Items.AIR || advCircuit == Items.AIR) return
            ShapedRecipeBuilder.shaped(RecipeCategory.EQUIPMENT, MiningLaserItem::class.instance(), 1)
                .pattern("REE")
                .pattern("AAC")
                .pattern(" AA")
                .define('R', Items.REDSTONE)
                .define('E', crystal)
                .define('A', alloy)
                .define('C', advCircuit)
                .unlockedBy(getHasName(advCircuit), has(advCircuit))
                .save(exporter, MiningLaserItem::class.id())
        }
    }

    // ========== IElectricTool 实现 ==========

    override val tier = 3
    override val maxCapacity: Long get() = Ic2Config.current.miningLaser.maxEnergy

    override fun getEnergy(stack: ItemStack): Long = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun inventoryTick(stack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient || entity !is ServerPlayer) return
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
            player.displayClientMessage(Component.translatable(mode.translationKey, false), true)
        }
        hadMiningLaserInMainLastTick[uuid] = true
    }

    // ========== 使用逻辑 ==========

    /**
     * 右键点击实体时不执行实体交互，仍发射激光。
     */
    override fun useOnEntity(stack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        return use(user.world, user, hand).result
    }

    /**
     * 右键点击方块时不触发方块交互，仍发射激光。
     */
    override fun useOn(context: UseOnContext): InteractionResult {
        return InteractionResult.PASS
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)

        // 客户端在按住模式键切模式时仍可能发来 use；与切模式包同 tick 的这一次不发射（见 [MiningLaserServerSuppress]）
        if (!world.isClient) {
            val sp = user as? ServerPlayer
            if (sp != null && MiningLaserServerSuppress.consumeSuppressNextFire(sp)) {
                return InteractionResultHolder.success(stack, false)
            }
        }

        val mode = getMode(stack)

        // 电量不足
        if (getEnergy(stack) < mode.energyCost) return InteractionResultHolder.pass(stack)

        if (!world.isClient) {
            val pitch = user.pitch
            val yaw = user.yaw

            if (mode.scatterCount > 1) {
                // 散射/3x3 模式：发射多个平行弹体，仅在起点做垂直散布
                val spread = mode.scatterSpread  // 散布总宽度（格）
                val count = mode.scatterCount
                val cols = kotlin.math.sqrt(count.toDouble()).toInt()
                val rows = (count + cols - 1) / cols

                // 计算视线方向的垂直平面基向量
                val radYaw = yaw.toDouble() * 0.017453292
                val radPitch = pitch.toDouble() * 0.017453292
                val fwdX = -Math.sin(radYaw) * Math.cos(radPitch)
                val fwdY = -Math.sin(radPitch)
                val fwdZ = Math.cos(radYaw) * Math.cos(radPitch)
                val forward = Vec3(fwdX, fwdY, fwdZ)
                // 右手向量：forward × up（竖直看时退化为用 X 轴做参考）
                val upRef = if (Math.abs(forward.y) > 0.99) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
                val right = forward.crossProduct(upRef).normalize()
                val perpUp = right.crossProduct(forward).normalize()

                var idx = 0
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        if (idx >= count) break
                        val dRight = (col - (cols - 1) / 2.0) * spread / cols.coerceAtLeast(1)
                        val dUp = (row - (rows - 1) / 2.0) * spread / rows.coerceAtLeast(1)
                        val offset = right.multiply(dRight).add(perpUp.multiply(dUp))
                        spawnProjectile(world, user, pitch, yaw, mode, offset)
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

        return InteractionResultHolder.success(stack)
    }

    private fun spawnProjectile(
        world: Level,
        owner: Player,
        pitch: Float,
        yaw: Float,
        laserMode: LaserMode,
        spreadOffset: Vec3 = Vec3.ZERO
    ) {
        val projectile = LaserProjectileEntity(ModEntities.LASER_PROJECTILE, world)
        projectile.init(owner, pitch, yaw, laserMode, spreadOffset)
        world.addFreshEntity(projectile)
    }

    // ========== Tooltip ==========
    // 模式、按键与说明由客户端 [ic2_120.client.MiningLaserTooltipHandler] 追加

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)

    override fun postMine(
        stack: ItemStack,
        world: Level,
        state: net.minecraft.world.level.block.state.BlockState,
        pos: net.minecraft.core.BlockPos,
        miner: LivingEntity
    ): Boolean = true
}

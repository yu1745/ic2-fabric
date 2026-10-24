package ic2_120.content.item

import ic2_120.compat.orCreateNbt

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.stack

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import ic2_120.content.block.TeleporterBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import java.util.function.Consumer
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

@ModItem(name = "recall_scroll", tab = CreativeTab.IC2_TOOLS, group = "tools")
class RecallScrollItem : Item(Item.Properties().stacksTo(1)) {
    companion object {
        private const val NBT_BIND_X = "BindX"
        private const val NBT_BIND_Y = "BindY"
        private const val NBT_BIND_Z = "BindZ"
        private const val NBT_BIND_DIM = "BindDim"
        private const val NBT_HAS_BIND = "HasBind"

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val teleporter = TeleporterBlock::class.item()
            val paper = Items.PAPER
            val advancedCircuit = AdvancedCircuit::class.instance()

            if (teleporter != Items.AIR && advancedCircuit != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, RecallScrollItem::class.instance(), 1)
                    .pattern("PAP")
                    .pattern("ATA")
                    .pattern("PAP")
                    .define('P', paper)
                    .define('A', advancedCircuit)
                    .define('T', teleporter)
                    .unlockedBy(getHasName(advancedCircuit), has(advancedCircuit))
                    .save(exporter, RecallScrollItem::class.id())
            }
        }
    }

    // ========== 绑定：右击传送机 ==========

    override fun useOn(context: UseOnContext): InteractionResult {
        val world = context.level
        val pos = context.pos
        val stack = context.stack
        val player = context.player
        val state = world.getBlockState(pos)

        if (state.block !is TeleporterBlock) return InteractionResult.PASS
        if (player == null) return InteractionResult.SUCCESS

        val nbt = stack.orCreateNbt
        val dim = world.dimension().value.toString()

        nbt.putBoolean(NBT_HAS_BIND, true)
        nbt.putInt(NBT_BIND_X, pos.x)
        nbt.putInt(NBT_BIND_Y, pos.y)
        nbt.putInt(NBT_BIND_Z, pos.z)
        nbt.putString(NBT_BIND_DIM, dim)
        if (!world.isClient) {
            player.displayClientMessage(Component.literal("已绑定传送机: ${pos.x}, ${pos.y}, ${pos.z}"), true)
        }
        return InteractionResult.SUCCESS
    }

    // ========== 使用：传送 ==========

    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(player.usedItemHand)
        if (world.isClient) return InteractionResultHolder.success(stack)

        val nbt = stack.orCreateNbt
        if (!nbt.getBoolean(NBT_HAS_BIND)) {
            player.displayClientMessage(Component.literal("请先绑定传送机。"), true)
            return InteractionResultHolder.fail(stack)
        }

        val dim = world.dimension().value.toString()
        val bindDim = nbt.getString(NBT_BIND_DIM)
        if (bindDim != dim) {
            player.displayClientMessage(Component.literal("维度不一致，无法传送。"), true)
            return InteractionResultHolder.fail(stack)
        }

        val bindPos = BlockPos(nbt.getInt(NBT_BIND_X), nbt.getInt(NBT_BIND_Y), nbt.getInt(NBT_BIND_Z))
        @Suppress("DEPRECATION")
        if (!world.isChunkLoaded(bindPos)) {
            player.displayClientMessage(Component.literal("目标坐标区块未加载。"), true)
            return InteractionResultHolder.fail(stack)
        }

        // 计算距离
        val distance = sqrt(player.pos.distanceToSqr(bindPos.x + 0.5, bindPos.y + 0.5, bindPos.z + 0.5))
            .toLong().coerceAtLeast(1L)
        val weight = computeWeight(player)
        val energyNeed = computeEnergyNeed(weight, distance)

        // 扫描背包可用能量
        val availableEnergy = scanAvailableEnergy(player)
        if (availableEnergy < energyNeed) {
            player.displayClientMessage(
                Component.literal("能量不足！需要 ${formatNumber(energyNeed)} EU，当前背包电池/工具共 ${formatNumber(availableEnergy)} EU"),
                true
            )
            return InteractionResultHolder.fail(stack)
        }

        // 从背包放电
        drainEnergyFromInventory(player, energyNeed)

        // 检查传送机是否还在
        val targetBlockState = world.getBlockState(bindPos)
        val teleporterExists = targetBlockState.block is TeleporterBlock

        // 传送
        val destX = bindPos.x + 0.5
        val destY = bindPos.y + 1.02
        val destZ = bindPos.z + 0.5
        player.teleportTo(destX, destY, destZ)
        player.velocity = Vec3.ZERO
        player.velocityModified = true

        if (teleporterExists) {
            player.displayClientMessage(Component.literal("传送成功！消耗 ${formatNumber(energyNeed, false)} EU"), true)
        } else {
            // 惩罚：传送机已遗失
            applyPenalty(player)
            player.displayClientMessage(Component.literal("锚点已遗失！"), true)
        }

        return InteractionResultHolder.success(stack)
    }

    // ========== Tooltip ==========

    override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, type)

        val nbt = stack.orCreateNbt
        if (nbt != null && nbt.getBoolean(NBT_HAS_BIND)) {
            val x = nbt.getInt(NBT_BIND_X)
            val y = nbt.getInt(NBT_BIND_Y)
            val z = nbt.getInt(NBT_BIND_Z)
            tooltip.add(Component.literal("已绑定: ($x, $y, $z)").withStyle(ChatFormatting.GRAY))
        } else {
            tooltip.add(Component.literal("未绑定").withStyle(ChatFormatting.GRAY))
        }
        tooltip.add(Component.literal("不要遗失锚点，否则会发生非常可怕的事").withStyle(ChatFormatting.RED))
    }

    // ========== 辅助方法 ==========

    private fun computeWeight(player: Player): Int {
        var weight = 1000
        weight += player.inventory.armor.count { !it.isEmpty } * 100

        var invWeight = 0.0
        for (stack in player.inventory.items) {
            if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxStackSize.toDouble().coerceAtLeast(1.0))
        }
        for (stack in player.inventory.offHand) {
            if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxStackSize.toDouble().coerceAtLeast(1.0))
        }
        weight += floor(invWeight).toInt()
        return weight.coerceAtMost(5100)
    }

    private fun computeEnergyNeed(weight: Int, distance: Long): Long {
        val x = floor(weight.toDouble()).coerceAtLeast(1.0)
        val l = floor(distance.toDouble()).coerceAtLeast(1.0)
        val y = floor(5.0 * x * (l + 10.0).pow(0.7))
        return y.toLong().coerceAtLeast(1L)
    }

    /**
     * 扫描玩家背包中所有 IBatteryItem 和 IElectricTool 的可用总能量。
     */
    private fun scanAvailableEnergy(player: Player): Long {
        var total = 0L
        // 优先 hotbar（0-8）
        for (slot in 0..8) {
            val stack = player.inventory.items[slot]
            if (stack.isEmpty) continue
            total += extractEnergyFromStack(stack, dryRun = true)
        }
        // 护甲栏
        for (slot in 0..<player.inventory.armor.size) {
            val stack = player.inventory.armor[slot]
            if (stack.isEmpty) continue
            total += extractEnergyFromStack(stack, dryRun = true)
        }
        // 其余主背包（9-35）
        for (slot in 9..<player.inventory.items.size) {
            val stack = player.inventory.items[slot]
            if (stack.isEmpty) continue
            total += extractEnergyFromStack(stack, dryRun = true)
        }
        // 副手
        val offhand = player.offhandItem
        if (!offhand.isEmpty) {
            total += extractEnergyFromStack(offhand, dryRun = true)
        }
        return total
    }

    /**
     * 从玩家背包中消耗指定量的能量。
     */
    private fun drainEnergyFromInventory(player: Player, amount: Long) {
        var remaining = amount
        // 优先 hotbar（0-8）
        for (slot in 0..8) {
            if (remaining <= 0) return
            val stack = player.inventory.items[slot]
            if (stack.isEmpty) continue
            remaining = doDrain(stack, remaining)
        }
        // 再扫护甲栏
        for (slot in 0..<player.inventory.armor.size) {
            if (remaining <= 0) return
            val stack = player.inventory.armor[slot]
            if (stack.isEmpty) continue
            remaining = doDrain(stack, remaining)
        }
        // 其余主背包（9-35）
        for (slot in 9..<player.inventory.items.size) {
            if (remaining <= 0) return
            val stack = player.inventory.items[slot]
            if (stack.isEmpty) continue
            remaining = doDrain(stack, remaining)
        }
        // 最后扫副手
        if (remaining > 0) {
            val offhand = player.offhandItem
            if (!offhand.isEmpty) {
                doDrain(offhand, remaining)
            }
        }
    }

    /**
     * 从一个物品栈中提取能量（或模拟提取）。
     * @param dryRun true=只计算可用能量，false=实际消耗
     */
    private fun extractEnergyFromStack(stack: ItemStack, dryRun: Boolean): Long {
        val battery = stack.item as? IBatteryItem
        if (battery != null) {
            return battery.getCurrentCharge(stack)
        }
        val tool = stack.item as? IElectricTool
        if (tool != null) {
            return tool.getEnergy(stack)
        }
        return 0L
    }

    /**
     * 从一个物品栈中消耗指定量的能量。
     * @return 未消耗完的剩余量
     */
    private fun doDrain(stack: ItemStack, amount: Long): Long {
        var remaining = amount
        val battery = stack.item as? IBatteryItem
        if (battery != null) {
            val drained = battery.discharge(stack, remaining)
            remaining -= drained
            return remaining
        }
        val tool = stack.item as? IElectricTool
        if (tool != null) {
            val current = tool.getEnergy(stack)
            val toDrain = minOf(current, remaining)
            tool.setEnergy(stack, current - toDrain)
            remaining -= toDrain
            return remaining
        }
        return remaining
    }

    /**
     * 传送机遗失惩罚：
     * - 玩家血量降至 1
     * - 电力护甲扣光 EU，耐久护甲消耗 1/2 剩余耐久
     */
    private fun applyPenalty(player: Player) {
        // 扣血
        player.setHealth(1f)

        // 处理护甲
        for (slot in 0..<player.inventory.armor.size) {
            val stack = player.inventory.armor[slot]
            if (stack.isEmpty) continue

            val tool = stack.item as? IElectricTool
            if (tool != null) {
                // 本模组电力护甲：扣光 EU
                tool.setEnergy(stack, 0)
            } else if (stack.isDamageable && stack.maxDamage > 0) {
                // 原版耐久护甲：消耗 1/2 剩余耐久
                val remaining = stack.maxDamage - stack.damage
                val consume = (remaining / 2).coerceAtLeast(1)
                stack.damageValue += consume
            }
        }
    }

    private fun formatNumber(n: Long): String = "%,d".format(n)
}

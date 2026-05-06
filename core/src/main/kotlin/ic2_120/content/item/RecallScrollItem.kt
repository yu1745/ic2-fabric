package ic2_120.content.item

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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.client.item.TooltipContext
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.function.Consumer
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

@ModItem(name = "recall_scroll", tab = CreativeTab.IC2_TOOLS, group = "tools")
class RecallScrollItem : Item(FabricItemSettings().maxCount(1)) {
    companion object {
        private const val NBT_BIND_X = "BindX"
        private const val NBT_BIND_Y = "BindY"
        private const val NBT_BIND_Z = "BindZ"
        private const val NBT_BIND_DIM = "BindDim"
        private const val NBT_HAS_BIND = "HasBind"

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val teleporter = TeleporterBlock::class.item()
            val paper = Items.PAPER
            val advancedCircuit = AdvancedCircuit::class.instance()

            if (teleporter != Items.AIR && advancedCircuit != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, RecallScrollItem::class.instance(), 1)
                    .pattern("PAP")
                    .pattern("ATA")
                    .pattern("PAP")
                    .input('P', paper)
                    .input('A', advancedCircuit)
                    .input('T', teleporter)
                    .criterion(hasItem(advancedCircuit), conditionsFromItem(advancedCircuit))
                    .offerTo(exporter, RecallScrollItem::class.id())
            }
        }
    }

    // ========== 绑定：右击传送机 ==========

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val stack = context.stack
        val player = context.player
        val state = world.getBlockState(pos)

        if (state.block !is TeleporterBlock) return ActionResult.PASS
        if (player == null) return ActionResult.SUCCESS

        val nbt = stack.orCreateNbt
        val dim = world.registryKey.value.toString()

        nbt.putBoolean(NBT_HAS_BIND, true)
        nbt.putInt(NBT_BIND_X, pos.x)
        nbt.putInt(NBT_BIND_Y, pos.y)
        nbt.putInt(NBT_BIND_Z, pos.z)
        nbt.putString(NBT_BIND_DIM, dim)
        if (!world.isClient) {
            player.sendMessage(Text.literal("已绑定传送机: ${pos.x}, ${pos.y}, ${pos.z}"), true)
        }
        return ActionResult.SUCCESS
    }

    // ========== 使用：传送 ==========

    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = player.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        val nbt = stack.orCreateNbt
        if (!nbt.getBoolean(NBT_HAS_BIND)) {
            player.sendMessage(Text.literal("请先绑定传送机。"), true)
            return TypedActionResult.fail(stack)
        }

        val dim = world.registryKey.value.toString()
        val bindDim = nbt.getString(NBT_BIND_DIM)
        if (bindDim != dim) {
            player.sendMessage(Text.literal("维度不一致，无法传送。"), true)
            return TypedActionResult.fail(stack)
        }

        val bindPos = BlockPos(nbt.getInt(NBT_BIND_X), nbt.getInt(NBT_BIND_Y), nbt.getInt(NBT_BIND_Z))
        if (!world.isChunkLoaded(bindPos)) {
            player.sendMessage(Text.literal("目标坐标区块未加载。"), true)
            return TypedActionResult.fail(stack)
        }

        // 计算距离
        val distance = sqrt(player.pos.squaredDistanceTo(bindPos.x + 0.5, bindPos.y + 0.5, bindPos.z + 0.5))
            .toLong().coerceAtLeast(1L)
        val weight = computeWeight(player)
        val energyNeed = computeEnergyNeed(weight, distance)

        // 扫描背包可用能量
        val availableEnergy = scanAvailableEnergy(player)
        if (availableEnergy < energyNeed) {
            player.sendMessage(
                Text.literal("能量不足！需要 ${formatNumber(energyNeed)} EU，当前背包电池/工具共 ${formatNumber(availableEnergy)} EU"),
                true
            )
            return TypedActionResult.fail(stack)
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
        player.requestTeleport(destX, destY, destZ)
        player.velocity = Vec3d.ZERO
        player.velocityModified = true

        if (teleporterExists) {
            player.sendMessage(Text.literal("传送成功！消耗 ${formatNumber(energyNeed)} EU"), true)
        } else {
            // 惩罚：传送机已遗失
            applyPenalty(player)
            player.sendMessage(Text.literal("锚点已遗失！"), true)
        }

        return TypedActionResult.success(stack)
    }

    // ========== Tooltip ==========

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)

        val nbt = stack.nbt
        if (nbt != null && nbt.getBoolean(NBT_HAS_BIND)) {
            val x = nbt.getInt(NBT_BIND_X)
            val y = nbt.getInt(NBT_BIND_Y)
            val z = nbt.getInt(NBT_BIND_Z)
            tooltip.add(Text.literal("已绑定: ($x, $y, $z)").formatted(Formatting.GRAY))
        } else {
            tooltip.add(Text.literal("未绑定").formatted(Formatting.GRAY))
        }
        tooltip.add(Text.literal("不要遗失锚点，否则会发生非常可怕的事").formatted(Formatting.RED))
    }

    // ========== 辅助方法 ==========

    private fun computeWeight(player: PlayerEntity): Int {
        var weight = 1000
        weight += player.inventory.armor.count { !it.isEmpty } * 100

        var invWeight = 0.0
        for (stack in player.inventory.main) {
            if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxCount.toDouble().coerceAtLeast(1.0))
        }
        for (stack in player.inventory.offHand) {
            if (!stack.isEmpty) invWeight += stack.count * (100.0 / stack.maxCount.toDouble().coerceAtLeast(1.0))
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
    private fun scanAvailableEnergy(player: PlayerEntity): Long {
        var total = 0L
        // 优先 hotbar（0-8）
        for (slot in 0..8) {
            val stack = player.inventory.main[slot]
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
        for (slot in 9..<player.inventory.main.size) {
            val stack = player.inventory.main[slot]
            if (stack.isEmpty) continue
            total += extractEnergyFromStack(stack, dryRun = true)
        }
        // 副手
        val offhand = player.offHandStack
        if (!offhand.isEmpty) {
            total += extractEnergyFromStack(offhand, dryRun = true)
        }
        return total
    }

    /**
     * 从玩家背包中消耗指定量的能量。
     */
    private fun drainEnergyFromInventory(player: PlayerEntity, amount: Long) {
        var remaining = amount
        // 优先 hotbar（0-8）
        for (slot in 0..8) {
            if (remaining <= 0) return
            val stack = player.inventory.main[slot]
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
        for (slot in 9..<player.inventory.main.size) {
            if (remaining <= 0) return
            val stack = player.inventory.main[slot]
            if (stack.isEmpty) continue
            remaining = doDrain(stack, remaining)
        }
        // 最后扫副手
        if (remaining > 0) {
            val offhand = player.offHandStack
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
    private fun applyPenalty(player: PlayerEntity) {
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
                stack.damage += consume
            }
        }
    }

    private fun formatNumber(n: Long): String = "%,d".format(n)
}

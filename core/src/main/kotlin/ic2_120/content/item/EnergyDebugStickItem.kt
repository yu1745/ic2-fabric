package ic2_120.content.item

import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.energy.EnergyNetworkManager
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.Direction
import team.reborn.energy.api.EnergyStorage

/**
 * 调试棒：仅可从创造模式物品栏获取。
 * 右键方块时显示该方块在 Tech Reborn Energy API 下的能量信息（当前储能、容量、是否可输入/输出）。
 */
@ModItem(name = "energy_debug_stick", tab = CreativeTab.IC2_TOOLS, group = "debug")
class EnergyDebugStickItem : Item(FabricItemSettings()) {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val player = context.player ?: return ActionResult.PASS
        if (world.isClient) return ActionResult.SUCCESS

        // 先尝试被点击的面，再尝试六个方向
        val directions = listOf(context.side) + Direction.values().toList()
        var storage: EnergyStorage? = null
        for (dir in directions) {
            val found = EnergyStorage.SIDED.find(world, pos, dir)
            if (found != null && found != EnergyStorage.EMPTY) {
                storage = found
                break
            }
        }

        if (storage == null) {
            player.sendMessage(
                net.minecraft.text.Text.translatable("message.ic2_120.energy_debug_stick.no_energy")
                    .formatted(Formatting.GRAY),
                true
            )
            return ActionResult.SUCCESS
        }

        val amount = storage.amount
        val capacity = storage.capacity
        val insert = storage.supportsInsertion()
        val extract = storage.supportsExtraction()
        val percent = if (capacity > 0) (100.0 * amount / capacity).toInt() else 0
        // 用事务模拟单次可插入/可提取量（不 commit，不改变实际存储）
        var maxInsertOnce = 0L
        var maxExtractOnce = 0L
        if (insert) {
            Transaction.openOuter().use { tx ->
                maxInsertOnce = storage.insert(Long.MAX_VALUE, tx)
            }
        }
        if (extract) {
            Transaction.openOuter().use { tx ->
                maxExtractOnce = storage.extract(Long.MAX_VALUE, tx)
            }
        }
        val msg = net.minecraft.text.Text.translatable(
            "message.ic2_120.energy_debug_stick.info",
            amount,
            capacity,
            percent,
            if (insert) "✓" else "—",
            if (extract) "✓" else "—",
            maxInsertOnce,
            maxExtractOnce
        ).formatted(Formatting.GREEN)
        player.sendMessage(msg, true)

        // 点击导线时额外显示电网等级与导线数
        if (world.getBlockState(pos).block is BaseCableBlock) {
            val network = EnergyNetworkManager.getOrCreateNetwork(world, pos)
            val networkMsg = net.minecraft.text.Text.translatable(
                "message.ic2_120.energy_debug_stick.network",
                network.outputLevel,
                network.cables.size
            ).formatted(Formatting.AQUA)
            player.sendMessage(networkMsg, true)
        }
        return ActionResult.SUCCESS
    }
}

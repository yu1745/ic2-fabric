package ic2_120.content

import ic2_120.Ic2_120
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.pipes.PipeNetworkManager
import ic2_120.content.item.energy.IElectricTool
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 扳手与机器方块的交互逻辑：
 * - 普通扳手左键拆机器：瞬间拆，掉完整机器，耗 10 耐久
 * - 电动扳手左键拆机器：瞬间拆，掉完整机器，耗 1000 EU（电量不足则无法拆卸）
 * - 扳手/电扳手右键：旋转机器朝向，不耗耐久/不耗电
 * - 非扳手拆卸：只掉机器外壳（由 MachineBlock.getCasingDrop 决定）
 */
object WrenchHandler {

    private val WRENCH_ID = Identifier(Ic2_120.MOD_ID, "wrench")
    private val ELECTRIC_WRENCH_ID = Identifier(Ic2_120.MOD_ID, "electric_wrench")

    fun isWrench(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val id = Registries.ITEM.getId(stack.item)
        return id == WRENCH_ID || id == ELECTRIC_WRENCH_ID
    }

    private fun isElectricWrench(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return Registries.ITEM.getId(stack.item) == ELECTRIC_WRENCH_ID
    }

    fun register() {
        // 右键：扳手旋转机器，不耗耐久
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            val stack = player.getStackInHand(hand)
            if (!isWrench(stack)) return@register ActionResult.PASS

            val pos = hitResult.blockPos
            val state = world.getBlockState(pos)
            val block = state.block

            if (block is BasePipeBlock) {
                if (!world.isClient) {
                    val be = world.getBlockEntity(pos) as? PipeBlockEntity ?: return@register ActionResult.PASS
                    be.toggleDisabled(hitResult.side)
                    val recomputed = block.recomputeState(world, pos, state, be)
                    world.setBlockState(pos, recomputed, Block.NOTIFY_ALL)
                    PipeNetworkManager.invalidateConnectionCachesAt(world, pos)
                    PipeNetworkManager.invalidateConnectionCachesAt(world, pos.offset(hitResult.side))
                }
                return@register ActionResult.SUCCESS
            }

            if (block !is MachineBlock) return@register ActionResult.PASS

            if (!world.isClient) {
                val facing = state.get(Properties.HORIZONTAL_FACING)
                val next = when (facing) {
                    Direction.NORTH -> Direction.EAST
                    Direction.EAST -> Direction.SOUTH
                    Direction.SOUTH -> Direction.WEST
                    Direction.WEST -> Direction.NORTH
                    else -> facing
                }
                world.setBlockState(pos, state.with(Properties.HORIZONTAL_FACING, next))
            }
            ActionResult.SUCCESS
        }

        // 左键：扳手瞬间拆机器，掉完整机器
        // - 普通扳手：耗 10 耐久
        // - 电动扳手：耗 1000 EU（电量不足则无法拆卸）
        AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
            val stack = player.getStackInHand(hand)
            if (!isWrench(stack)) return@register ActionResult.PASS

            val state = world.getBlockState(pos)
            if (state.block !is MachineBlock) return@register ActionResult.PASS

            // 电动扳手：电量不足 1000 EU 则不允许拆卸
            if (isElectricWrench(stack)) {
                val tool = stack.item as IElectricTool
                if (tool.getEnergy(stack) < 1000) return@register ActionResult.FAIL
            }

            if (!world.isClient && player is ServerPlayerEntity) {
                // 副手扳手时临时换到主手，确保 loot 表识别为扳手拆卸
                val swapped = hand == Hand.OFF_HAND
                if (swapped) {
                    val main = player.mainHandStack
                    player.setStackInHand(Hand.MAIN_HAND, stack)
                    player.setStackInHand(Hand.OFF_HAND, main)
                }
                val broken = world.breakBlock(pos, true, player)
                if (swapped) {
                    val main = player.mainHandStack
                    player.setStackInHand(Hand.MAIN_HAND, player.getStackInHand(Hand.OFF_HAND))
                    player.setStackInHand(Hand.OFF_HAND, main)
                }
                if (broken) {
                    when {
                        isElectricWrench(stack) -> {
                            val tool = stack.item as IElectricTool
                            val current = tool.getEnergy(stack)
                            tool.setEnergy(stack, current - 1000)
                        }
                        stack.isDamageable -> {
                            stack.damage(10, player) { it.sendToolBreakStatus(hand) }
                        }
                    }
                }
            }
            ActionResult.SUCCESS
        }
    }
}

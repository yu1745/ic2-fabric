package ic2_120.content

import ic2_120.Ic2_120
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.pipes.PipeNetworkManager
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.TankBlock
import ic2_120.content.block.storage.TankBlockEntity
import ic2_120.content.item.energy.IElectricTool
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.nbt.NbtCompound
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
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
 * - 扳手/电扳手右键：正面朝向玩家，Shift+右键：背面朝向玩家，不耗耐久/不耗电
 * - 非扳手拆卸：只掉机器外壳（由 MachineBlock.getCasingDrop 决定）
 *
 * 注意：World.breakBlock(..., drop=true, player) 内部对掉落使用 ItemStack.EMPTY 作为 TOOL，
 * match_tool（扳手）战利品条件不会生效。扳手拆机需先 breakBlock(false) 再 Block.dropStacks(..., 扳手)。
 *
 * 储罐：扳手拆卸保留 80% 流体
 */
object WrenchHandler {

    private val WRENCH_ID = Identifier(Ic2_120.MOD_ID, "wrench")
    private val ELECTRIC_WRENCH_ID = Identifier(Ic2_120.MOD_ID, "electric_wrench")
    private val WRENCH_USE_SOUND = SoundEvent.of(Identifier("ic2", "item.wrench.use"))
    private const val ENERGY_RETAIN_RATIO = 0.8

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
        // 右键：扳手设置机器朝向，不耗耐久
            // 右键正面朝向玩家，Shift+右键背面朝向玩家
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            val stack = player.getStackInHand(hand)
            if (!isWrench(stack)) return@register ActionResult.PASS

            val pos = hitResult.blockPos
            val state = world.getBlockState(pos)
            val block = state.block

            if (block is BasePipeBlock) {
                if (!world.isClient) {
                    if (player.isSneaking) {
                        val transparent = !state.get(BasePipeBlock.TRANSPARENT)
                        world.setBlockState(pos, state.with(BasePipeBlock.TRANSPARENT, transparent), Block.NOTIFY_ALL)
                        world.playSound(null, pos, WRENCH_USE_SOUND, SoundCategory.BLOCKS, 1.0f, 1.0f)
                    } else {
                        val be = world.getBlockEntity(pos) as? PipeBlockEntity ?: return@register ActionResult.PASS
                        be.toggleDisabled(hitResult.side)
                        val recomputed = block.recomputeState(world, pos, state, be)
                        world.setBlockState(pos, recomputed, Block.NOTIFY_ALL)
                        PipeNetworkManager.invalidateConnectionCachesAt(world, pos)
                        PipeNetworkManager.invalidateConnectionCachesAt(world, pos.offset(hitResult.side))
                    }
                }
                return@register ActionResult.SUCCESS
            }

            if (block !is MachineBlock) return@register ActionResult.PASS

            if (!world.isClient) {
                val target = if (player.isSneaking) player.horizontalFacing else player.horizontalFacing.opposite
                world.setBlockState(pos, state.with(Properties.HORIZONTAL_FACING, target))
                world.playSound(null, pos, WRENCH_USE_SOUND, SoundCategory.BLOCKS, 1.0f, 1.0f)
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
            val block = state.block

            // 储罐：扳手拆卸保留 80% 流体
            if (block is TankBlock) {
                if (!world.isClient) {
                    val be = world.getBlockEntity(pos) as? TankBlockEntity
                    be?.retainFluidPercent(0.8)
                }
                // fall through to normal break
            }

            if (block !is MachineBlock && block !is TankBlock) return@register ActionResult.PASS

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
                val broken = if (block is EnergyStorageBlock) {
                    val energyBe = world.getBlockEntity(pos) as? EnergyStorageBlockEntity
                    val retained = ((energyBe?.sync?.amount ?: 0L) * ENERGY_RETAIN_RATIO).toLong().coerceAtLeast(0L)
                    val didBreak = world.breakBlock(pos, false, player)
                    if (didBreak) {
                        val dropped = ItemStack(block.asItem())
                        if (retained > 0L) {
                            val blockEntityTag = NbtCompound()
                            blockEntityTag.putLong(
                                ic2_120.content.sync.EnergyStorageSync.NBT_ENERGY_STORED,
                                retained
                            )
                            dropped.orCreateNbt.put(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG, blockEntityTag)
                        }
                        val itemEntity = net.minecraft.entity.ItemEntity(
                            world,
                            pos.x.toDouble() + 0.5,
                            pos.y.toDouble() + 0.5,
                            pos.z.toDouble() + 0.5,
                            dropped
                        )
                        itemEntity.setToDefaultPickupDelay()
                        world.spawnEntity(itemEntity)
                    }
                    didBreak
                } else {
                    val stateBefore = state
                    val be = if (stateBefore.hasBlockEntity()) world.getBlockEntity(pos) else null
                    val didBreak = world.breakBlock(pos, false, player)
                    if (didBreak && world is ServerWorld) {
                        Block.dropStacks(stateBefore, world, pos, be, player, stack.copy())
                    }
                    didBreak
                }
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
                    world.playSound(null, pos, WRENCH_USE_SOUND, SoundCategory.BLOCKS, 1.0f, 1.0f)
                }
            }
            ActionResult.SUCCESS
        }
    }
}

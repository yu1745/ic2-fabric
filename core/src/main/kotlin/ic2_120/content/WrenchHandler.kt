package ic2_120.content

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.compat.orCreateNbt

import ic2_120.compat.isClient

import ic2_120.Ic2_120
import ic2_120.content.block.DirectionalMachineBlock
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.PatternStorageBlock
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.block.pipes.BasePipeBlock
import ic2_120.content.block.pipes.PipeBlockEntity
import ic2_120.content.block.pipes.PipeNetworkManager
import ic2_120.content.block.storage.EnergyStorageBlock
import ic2_120.content.block.storage.EnergyStorageBlockEntity
import ic2_120.content.block.storage.StorageBoxBlock
import ic2_120.content.block.storage.TankBlock
import ic2_120.content.block.storage.TankBlockEntity
import ic2_120.content.item.energy.IElectricTool
import ic2_120.integration.ftbchunks.ClaimProtection
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.block.Block
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Items
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * 扳手与机器方块的交互逻辑：
 * - 普通扳手左键拆机器：瞬间拆，掉完整机器，耗 10 耐久
 * - 电动扳手左键拆机器：瞬间拆，掉完整机器，耗 1000 EU（电量不足则无法拆卸）
 * - 扳手/电扳手右键：正面朝向玩家，Shift+右键：背面朝向玩家，不耗耐久/不耗电
 * - 非扳手拆卸：只掉机器外壳（由 MachineBlock.getCasingDrop 决定）
 *
 * 注意：Level.destroyBlock(..., drop=true, player) 内部对掉落使用 ItemStack.EMPTY 作为 TOOL，
 * match_tool（扳手）战利品条件不会生效。扳手拆机需先 breakBlock(false) 再 Block.dropResources(..., 扳手)。
 *
 * 储罐：扳手拆卸保留 80% 流体
 */
object WrenchHandler {

    private val WRENCH_ID = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "wrench")
    private val ELECTRIC_WRENCH_ID = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "electric_wrench")
    private val WRENCH_USE_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ic2", "item.wrench.use"))
    private const val ENERGY_RETAIN_RATIO = 0.8

    fun isWrench(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val id = BuiltInRegistries.ITEM.getKey(stack.item)
        return id == WRENCH_ID || id == ELECTRIC_WRENCH_ID
    }

    private fun isElectricWrench(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return BuiltInRegistries.ITEM.getKey(stack.item) == ELECTRIC_WRENCH_ID
    }

    fun register() {
        // 右键：扳手设置机器朝向，不耗耐久
            // 右键正面朝向玩家，Shift+右键背面朝向玩家
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            val stack = player.getItemInHand(hand)
            if (!isWrench(stack)) return@register InteractionResult.PASS

            val pos = hitResult.blockPos
            val state = world.getBlockState(pos)
            val block = state.block

            if (block is BasePipeBlock) {
                if (!world.isClient) {
                    if (ClaimProtection.isProtected(world, pos, player, ClaimProtection.INTERACT_BLOCK)) {
                        return@register InteractionResult.FAIL
                    }
                    if (player.isCrouching) {
                        val transparent = !state.getValue(BasePipeBlock.TRANSPARENT)
                        world.setBlock(pos, state.setValue(BasePipeBlock.TRANSPARENT, transparent), Block.UPDATE_ALL)
                        world.playSound(null, pos, WRENCH_USE_SOUND, SoundSource.BLOCKS, 1.0f, 1.0f)
                    } else {
                        val be = world.getBlockEntity(pos) as? PipeBlockEntity ?: return@register InteractionResult.PASS
                        be.toggleDisabled(hitResult.direction)
                        val recomputed = block.recomputeState(world, pos, state, be)
                        world.setBlock(pos, recomputed, Block.UPDATE_ALL)
                        PipeNetworkManager.invalidateConnectionCachesAt(world, pos)
                        PipeNetworkManager.invalidateConnectionCachesAt(world, pos.relative(hitResult.direction))
                    }
                }
                return@register InteractionResult.SUCCESS
            }

            if (block !is MachineBlock && block !is DirectionalMachineBlock) return@register InteractionResult.PASS

            if (!world.isClient) {
                if (ClaimProtection.isProtected(world, pos, player, ClaimProtection.INTERACT_BLOCK)) {
                    return@register InteractionResult.FAIL
                }
                if (isElectricWrench(stack)) {
                    val tool = stack.item as IElectricTool
                    if (tool.getEnergy(stack) < 100L) return@register InteractionResult.FAIL
                    tool.setEnergy(stack, tool.getEnergy(stack) - 100L)
                }
                when (block) {
                    is DirectionalMachineBlock -> {
                        // 变压器等六面朝向机器：根据玩家视角计算朝向
                        val fullFacing = when {
                            player.xRot < -45f -> Direction.UP
                            player.xRot > 45f -> Direction.DOWN
                            else -> player.direction
                        }
                        val target = if (player.isCrouching) fullFacing else fullFacing.opposite
                        world.setBlock(pos, state.setValue(BlockStateProperties.FACING, target), 3)
                    }
                    is MachineBlock -> {
                        val target = if (player.isCrouching) player.direction else player.direction.opposite
                        world.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING, target), 3)
                    }
                }
                world.playSound(null, pos, WRENCH_USE_SOUND, SoundSource.BLOCKS, 1.0f, 1.0f)
            }
            InteractionResult.SUCCESS
        }

        // 左键：扳手瞬间拆机器，掉完整机器
        // - 普通扳手：耗 10 耐久
        // - 电动扳手：耗 1000 EU（电量不足则无法拆卸）
        AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
            val stack = player.getItemInHand(hand)
            if (!isWrench(stack)) return@register InteractionResult.PASS

            val state = world.getBlockState(pos)
            val block = state.block

            if (!world.isClient && ClaimProtection.isProtected(world, pos, player, ClaimProtection.EDIT_BLOCK)) {
                return@register InteractionResult.FAIL
            }

            // 储罐：扳手拆卸保留 80% 流体
            if (block is TankBlock) {
                if (!world.isClient) {
                    val be = world.getBlockEntity(pos) as? TankBlockEntity
                    be?.retainFluidPercent(0.8)
                }
                // fall through to normal break
            }

            if (block !is MachineBlock && block !is DirectionalMachineBlock && block !is TankBlock && block !is StorageBoxBlock) return@register InteractionResult.PASS

            // 电动扳手：电量不足 1000 EU 则不允许拆卸
            if (isElectricWrench(stack)) {
                val tool = stack.item as IElectricTool
                if (tool.getEnergy(stack) < 1000) return@register InteractionResult.FAIL
            }

            if (!world.isClient && player is ServerPlayer) {
                // 副手扳手时临时换到主手，确保 loot 表识别为扳手拆卸
                val swapped = hand == InteractionHand.OFF_HAND
                if (swapped) {
                    val main = player.mainHandItem
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack)
                    player.setItemInHand(InteractionHand.OFF_HAND, main)
                }
                val broken = if (block is EnergyStorageBlock) {
                    val energyBe = world.getBlockEntity(pos) as? EnergyStorageBlockEntity
                    val retained = ((energyBe?.sync?.amount ?: 0L) * ENERGY_RETAIN_RATIO).toLong().coerceAtLeast(0L)
                    val didBreak = world.destroyBlock(pos, false, player)
                    if (didBreak) {
                        val dropped = ItemStack(block.asItem())
                        if (retained > 0L) {
                            val blockEntityTag = CompoundTag()
                            blockEntityTag.putLong(
                                ic2_120.content.sync.EnergyStorageSync.NBT_ENERGY_STORED,
                                retained
                            )
                            dropped.orCreateNbt = dropped.orCreateNbt.apply { put(EnergyStorageBlock.NBT_BLOCK_ENTITY_TAG, blockEntityTag) }
                        }
                        val itemEntity = net.minecraft.world.entity.item.ItemEntity(
                            world,
                            pos.x.toDouble() + 0.5,
                            pos.y.toDouble() + 0.5,
                            pos.z.toDouble() + 0.5,
                            dropped
                        )
                        itemEntity.setDefaultPickUpDelay()
                        world.addFreshEntity(itemEntity)
                    }
                    didBreak
                } else if (block is PatternStorageBlock) {
                    val patternBe = world.getBlockEntity(pos) as? PatternStorageBlockEntity
                    // 水晶要爆出来，不保存在 NBT 里
                    val crystalStack = patternBe?.removeItem(PatternStorageBlockEntity.SLOT_CRYSTAL, 64)
                    val nbt = patternBe?.saveWithoutMetadata(world.registryAccess())
                    world.removeBlockEntity(pos)
                    val didBreak = world.destroyBlock(pos, false, player)
                    if (didBreak && nbt != null) {
                        val dropped = ItemStack(block.asItem())
                        if (!nbt.isEmpty) {
                            dropped.orCreateNbt = dropped.orCreateNbt.apply { put("BlockEntityTag", nbt) }
                        }
                        val patternEntity = net.minecraft.world.entity.item.ItemEntity(
                            world,
                            pos.x.toDouble() + 0.5,
                            pos.y.toDouble() + 0.5,
                            pos.z.toDouble() + 0.5,
                            dropped
                        )
                        patternEntity.setDefaultPickUpDelay()
                        world.addFreshEntity(patternEntity)
                        // 水晶单独掉落
                        if (crystalStack != null && !crystalStack.isEmpty) {
                            val crystalEntity = net.minecraft.world.entity.item.ItemEntity(
                                world,
                                pos.x.toDouble() + 0.5,
                                pos.y.toDouble() + 0.5,
                                pos.z.toDouble() + 0.5,
                                crystalStack
                            )
                            crystalEntity.setDefaultPickUpDelay()
                            world.addFreshEntity(crystalEntity)
                        }
                    }
                    didBreak
                } else if (block is StorageBoxBlock) {
                    world.destroyBlock(pos, false, player)
                    true
                } else {
                    val stateBefore = state
                    val be = if (stateBefore.hasBlockEntity()) world.getBlockEntity(pos) else null
                    val didBreak = world.destroyBlock(pos, false, player)
                    if (didBreak && world is ServerLevel) {
                        Block.dropResources(stateBefore, world, pos, be, player, stack.copy())
                    }
                    didBreak
                }
                if (swapped) {
                    val main = player.mainHandItem
                    player.setItemInHand(InteractionHand.MAIN_HAND, player.getItemInHand(InteractionHand.OFF_HAND))
                    player.setItemInHand(InteractionHand.OFF_HAND, main)
                }
                if (broken) {
                    when {
                        isElectricWrench(stack) -> {
                            val tool = stack.item as IElectricTool
                            val current = tool.getEnergy(stack)
                            tool.setEnergy(stack, current - 1000)
                        }
                        stack.isDamageableItem -> {
                            stack.hurtAndBreak(
                                10,
                                player,
                                if (hand == InteractionHand.MAIN_HAND) net.minecraft.world.entity.EquipmentSlot.MAINHAND else net.minecraft.world.entity.EquipmentSlot.OFFHAND
                            )
                        }
                    }
                    world.playSound(null, pos, WRENCH_USE_SOUND, SoundSource.BLOCKS, 1.0f, 1.0f)
                }
            }
            InteractionResult.SUCCESS
        }
    }
}

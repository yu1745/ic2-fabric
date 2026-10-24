package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.AdvancedMinerBlockEntity
import ic2_120.content.block.machines.MinerBlockEntity
import ic2_120.integration.ftbchunks.ClaimProtection
import ic2_120.content.item.Alloy
import ic2_120.content.item.Circuit
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.id
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ic2_120.content.block.machines.BaseMinerBlockEntity
import ic2_120.content.item.AdvancedScannerItem
import ic2_120.content.item.OdScannerItem
import ic2_120.content.item.ScannerType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.ItemStack
import net.minecraft.world.Containers

// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
abstract class BaseMinerBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { player.openMenu(it) }
        }
        return InteractionResult.SUCCESS
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.`is`(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is BaseMinerBlockEntity) {
                for (stack in be.itemCache) {
                    if (!stack.isEmpty) net.minecraft.world.Containers.dropItemStack(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack.copy())
                }
                be.itemCache.clear()
                be.cacheItemCount = 0
            }
            retrieveMiningPipes(world, pos)
        }
        super.onRemove(state, world, pos, newState, moved)
    }

    private fun retrieveMiningPipes(world: Level, pos: BlockPos) {
        // 读取采矿机的扫描范围，用于回收水平方向铺设的管道
        val be = world.getBlockEntity(pos)
        val radius = if (be is BaseMinerBlockEntity) {
            val scanner = be.getItem(BaseMinerBlockEntity.SLOT_SCANNER)
            when (scanner.item) {
                is OdScannerItem -> ScannerType.OD.scanRadius
                is AdvancedScannerItem -> ScannerType.OV.scanRadius
                else -> 0
            }
        } else 0

        val planned = mutableListOf<BlockPos>()
        for (y in (pos.y - 1) downTo world.minBuildHeight) {
            var foundAny = false

            // 垂直管柱（直上直下的管道）
            val shaftPos = BlockPos(pos.x, y, pos.z)
            val shaftState = world.getBlockState(shaftPos)
            if (shaftState.block is MiningPipeBlock) {
                planned.add(shaftPos)
                foundAny = true
            } else if (radius <= 0) {
                break  // 无扫描仪，仅回收垂直管柱后停止
            }

            // 水平方向管道（在扫描范围内回收）
            if (radius > 0) {
                for (x in -radius..radius) {
                    for (z in -radius..radius) {
                        if (x == 0 && z == 0) continue  // 垂直管柱已处理
                        val pipePos = BlockPos(pos.x + x, y, pos.z + z)
                        val state = world.getBlockState(pipePos)
                        if (state.block is MiningPipeBlock) {
                            planned.add(pipePos)
                            foundAny = true
                        }
                    }
                }
            }

            if (!foundAny) break  // 此层及以下无管道，停止回收
        }
        if (!ClaimProtection.allAllowedUuid(world, planned, null, ClaimProtection.EDIT_BLOCK)) return
        for (pipePos in planned) {
            val state = world.getBlockState(pipePos)
            if (state.block !is MiningPipeBlock) continue
            Containers.dropItemStack(world, pipePos.x.toDouble(), pipePos.y.toDouble(), pipePos.z.toDouble(), ItemStack(state.block.asItem()))
            world.setBlock(pipePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
        }
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")
    }
}

@ModBlock(name = "miner", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "resource")
class MinerBlock : BaseMinerBlock() {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = MinerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MinerBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val miningPipe = MiningPipeBlock::class.item()
            val chest = Items.CHEST
            if (machine != Items.AIR && circuit != Items.AIR && miningPipe != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MinerBlock::class.item(), 1)
                    .pattern(" C ")
                    .pattern("cMc")
                    .pattern(" P ")
                    .define('C', chest)
                    .define('c', circuit)
                    .define('M', machine)
                    .define('P', miningPipe)
                    .unlockedBy(getHasName(machine), has(machine))
                    .save(exporter, MinerBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "advanced_miner", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "resource")
class AdvancedMinerBlock : BaseMinerBlock() {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = AdvancedMinerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, AdvancedMinerBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val alloy = Alloy::class.instance()
            val miner = MinerBlock::class.item()
            val teleporter = TeleporterBlock::class.item()
            val advCasing = AdvancedMachineCasingBlock::class.item()
            val mfe = MfeBlock::class.item()
            if (alloy != Items.AIR && miner != Items.AIR && teleporter != Items.AIR && advCasing != Items.AIR && mfe != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AdvancedMinerBlock::class.item(), 1)
                    .pattern("AmA")
                    .pattern("TUM")
                    .pattern("AmA")
                    .define('A', alloy)
                    .define('m', miner)
                    .define('T', teleporter)
                    .define('U', advCasing)
                    .define('M', mfe)
                    .unlockedBy(getHasName(miner), has(miner))
                    .save(exporter, AdvancedMinerBlock::class.id())
            }
        }
    }
}

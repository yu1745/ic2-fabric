package ic2_120.content.block

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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
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
abstract class BaseMinerBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {
    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: Level,
        pos: BlockPos
    ): net.minecraft.world.MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.world.MenuProvider
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return InteractionResult.SUCCESS
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is BaseMinerBlockEntity) {
                for (stack in be.itemCache) {
                    if (!stack.isEmpty) net.minecraft.world.Containers.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack.copy())
                }
                be.itemCache.clear()
                be.cacheItemCount = 0
            }
            retrieveMiningPipes(world, pos)
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    private fun retrieveMiningPipes(world: Level, pos: BlockPos) {
        // 读取采矿机的扫描范围，用于回收水平方向铺设的管道
        val be = world.getBlockEntity(pos)
        val radius = if (be is BaseMinerBlockEntity) {
            val scanner = be.getStack(BaseMinerBlockEntity.SLOT_SCANNER)
            when (scanner.item) {
                is OdScannerItem -> ScannerType.OD.scanRadius
                is AdvancedScannerItem -> ScannerType.OV.scanRadius
                else -> 0
            }
        } else 0

        val planned = mutableListOf<BlockPos>()
        for (y in (pos.y - 1) downTo world.bottomY) {
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
            Containers.spawn(world, pipePos.x.toDouble(), pipePos.y.toDouble(), pipePos.z.toDouble(), ItemStack(state.block.asItem()))
            world.setBlockState(pipePos, Blocks.AIR.defaultState, Block.NOTIFY_ALL)
        }
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

@ModBlock(name = "miner", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "resource")
class MinerBlock : BaseMinerBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = MinerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, MinerBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val miningPipe = MiningPipeBlock::class.item()
            val chest = Items.CHEST
            if (machine != Items.AIR && circuit != Items.AIR && miningPipe != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, MinerBlock::class.item(), 1)
                    .pattern(" C ")
                    .pattern("cMc")
                    .pattern(" P ")
                    .input('C', chest)
                    .input('c', circuit)
                    .input('M', machine)
                    .input('P', miningPipe)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, MinerBlock::class.id())
            }
        }
    }
}

@ModBlock(name = "advanced_miner", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "resource")
class AdvancedMinerBlock : BaseMinerBlock() {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = AdvancedMinerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, AdvancedMinerBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val alloy = Alloy::class.instance()
            val miner = MinerBlock::class.item()
            val teleporter = TeleporterBlock::class.item()
            val advCasing = AdvancedMachineCasingBlock::class.item()
            val mfe = MfeBlock::class.item()
            if (alloy != Items.AIR && miner != Items.AIR && teleporter != Items.AIR && advCasing != Items.AIR && mfe != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, AdvancedMinerBlock::class.item(), 1)
                    .pattern("AmA")
                    .pattern("TUM")
                    .pattern("AmA")
                    .input('A', alloy)
                    .input('m', miner)
                    .input('T', teleporter)
                    .input('U', advCasing)
                    .input('M', mfe)
                    .criterion(hasItem(miner), conditionsFromItem(miner))
                    .offerTo(exporter, AdvancedMinerBlock::class.id())
            }
        }
    }
}

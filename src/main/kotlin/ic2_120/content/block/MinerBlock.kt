package ic2_120.content.block

import ic2_120.content.block.machines.AdvancedMinerBlockEntity
import ic2_120.content.block.machines.MinerBlockEntity
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
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import ic2_120.content.block.machines.BaseMinerBlockEntity
import ic2_120.content.item.AdvancedScannerItem
import ic2_120.content.item.OdScannerItem
import ic2_120.content.item.ScannerType
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.ItemScatterer

abstract class BaseMinerBlock : MachineBlock() {
    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: World,
        pos: BlockPos
    ): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
        }
        return ActionResult.SUCCESS
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            retrieveMiningPipes(world, pos)
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    private fun retrieveMiningPipes(world: World, pos: BlockPos) {
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

        for (y in (pos.y - 1) downTo world.bottomY) {
            var foundAny = false

            // 垂直管柱（直上直下的管道）
            val shaftPos = BlockPos(pos.x, y, pos.z)
            val shaftState = world.getBlockState(shaftPos)
            if (shaftState.block is MiningPipeBlock) {
                ItemScatterer.spawn(world, shaftPos.x.toDouble(), shaftPos.y.toDouble(), shaftPos.z.toDouble(), ItemStack(shaftState.block.asItem()))
                world.setBlockState(shaftPos, Blocks.AIR.defaultState, Block.NOTIFY_ALL)
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
                            ItemScatterer.spawn(world, pipePos.x.toDouble(), pipePos.y.toDouble(), pipePos.z.toDouble(), ItemStack(state.block.asItem()))
                            world.setBlockState(pipePos, Blocks.AIR.defaultState, Block.NOTIFY_ALL)
                            foundAny = true
                        }
                    }
                }
            }

            if (!foundAny) break  // 此层及以下无管道，停止回收
        }
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

@ModBlock(name = "miner", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "resource")
class MinerBlock : BaseMinerBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = MinerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MinerBlockEntity::class.type()){ w, p, s, be ->
            (be as MinerBlockEntity).tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val miningPipe = MiningPipeBlock::class.item()
            val chest = Items.CHEST
            if (machine != Items.AIR && circuit != Items.AIR && miningPipe != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MinerBlock::class.item(), 1)
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

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, AdvancedMinerBlockEntity::class.type()){ w, p, s, be ->
            (be as AdvancedMinerBlockEntity).tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val alloy = Alloy::class.instance()
            val miner = MinerBlock::class.item()
            val teleporter = TeleporterBlock::class.item()
            val advCasing = AdvancedMachineCasingBlock::class.item()
            val mfe = MfeBlock::class.item()
            if (alloy != Items.AIR && miner != Items.AIR && teleporter != Items.AIR && advCasing != Items.AIR && mfe != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdvancedMinerBlock::class.item(), 1)
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

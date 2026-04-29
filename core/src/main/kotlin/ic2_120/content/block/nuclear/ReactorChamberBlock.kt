package ic2_120.content.block.nuclear

import ic2_120.Ic2_120
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.MachineCasingBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 核反应仓。单独放置无 UI、无容量。
 * 与核反应堆相邻时，右键等效于右键反应堆，打开反应堆的 UI。
 */
@ModBlock(name = "reactor_chamber", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorChamberBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        for (dir in Direction.values()) {
            val neighborPos = pos.offset(dir)
            val neighborState = world.getBlockState(neighborPos)
            if (neighborState.block is NuclearReactorBlock) {
                val be = world.getBlockEntity(neighborPos)
                if (be is net.minecraft.screen.NamedScreenHandlerFactory) {
                    player.openHandledScreen(be)
                    return ActionResult.SUCCESS
                }
            }
        }
        return ActionResult.PASS
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ReactorChamberBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (world.isClient) null
        else BlockEntityTicker { world, pos, state, blockEntity ->
            if (blockEntity is ReactorChamberBlockEntity) {
                ReactorChamberBlockEntity.tick(world, pos, state, blockEntity)
            }
        }
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val machine = MachineCasingBlock::class.item()
            val leadPlate = ic2_120.content.item.LeadPlate::class.instance()
            if (machine != Items.AIR && leadPlate != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorChamberBlock::class.item(), 1)
                    .pattern("L L").pattern(" M ").pattern("L L")
                    .input('L', leadPlate).input('M', machine)
                    .criterion(hasItem(leadPlate), conditionsFromItem(leadPlate))
                    .offerTo(exporter, ReactorChamberBlock::class.id())
            }
        }
    }
}

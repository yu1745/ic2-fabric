package ic2_120.content.block.nuclear

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.compat.has

import ic2_120.compat.isClient

import ic2_120.Ic2_120
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.MachineCasingBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 核反应仓。单独放置无 UI、无容量。
 * 与核反应堆相邻时，右键等效于右键反应堆，打开反应堆的 UI。
 */
@ModBlock(name = "reactor_chamber", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorChamberBlock(settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        for (dir in Direction.values()) {
            val neighborPos = pos.relative(dir)
            val neighborState = world.getBlockState(neighborPos)
            if (neighborState.block is NuclearReactorBlock) {
                val be = world.getBlockEntity(neighborPos)
                if (be is net.minecraft.world.MenuProvider) {
                    player.openMenu(be)
                    return InteractionResult.SUCCESS
                }
            }
        }
        return InteractionResult.PASS
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ReactorChamberBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
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
        fun generateRecipes(exporter: RecipeOutput) {
            val machine = MachineCasingBlock::class.item()
            val leadPlate = ic2_120.content.item.LeadPlate::class.instance()
            if (machine != Items.AIR && leadPlate != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ReactorChamberBlock::class.item(), 1)
                    .pattern("L L").pattern(" M ").pattern("L L")
                    .define('L', leadPlate).define('M', machine)
                    .unlockedBy(getHasName(leadPlate), has(leadPlate))
                    .save(exporter, ReactorChamberBlock::class.id())
            }
        }
    }
}

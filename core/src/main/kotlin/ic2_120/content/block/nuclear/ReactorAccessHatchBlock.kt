package ic2_120.content.block.nuclear

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.tags.ItemTags

/**
 * 反应堆访问接口。在反应堆结构内时，右键可打开反应堆 UI；Container 委托到对应的中心核反应堆。
 */
@ModBlock(name = "reactor_access_hatch", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor", renderLayer = "translucent")
class ReactorAccessHatchBlock(settings: BlockBehaviour.Properties = ReactorShellBlock.shellSettings()) : ReactorShellBlock(settings) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ReactorAccessHatchBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (world.isClient) null
        else BlockEntityTicker { world, pos, state, blockEntity ->
            if (blockEntity is ReactorAccessHatchBlockEntity) {
                ReactorAccessHatchBlockEntity.tick(world, pos, state, blockEntity)
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        // 查找反应堆，仅当访问接口在其 5×5×5 结构内部时才打开 UI（不根据距离，根据结构归属）
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val reactorPos = pos.offset(dx, dy, dz)
                    val be = world.getBlockEntity(reactorPos)
                    if (be is NuclearReactorBlockEntity && be.isPositionInStructure(pos)) {
                        player.openMenu(be)
                        return InteractionResult.SUCCESS
                    }
                }
            }
        }
        return InteractionResult.PASS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val vessel = ReactorVesselBlock::class.item()
            if (vessel != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ReactorAccessHatchBlock::class.item(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .define('A', vessel)
                    .define('B', ItemTags.WOODEN_TRAPDOORS)
                    .unlockedBy(getHasName(vessel), has(vessel))
                    .save(exporter, ReactorAccessHatchBlock::class.id())
            }
        }
    }
}

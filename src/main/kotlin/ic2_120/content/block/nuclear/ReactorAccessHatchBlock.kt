package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

/**
 * 反应堆访问接口。在反应堆结构内时，右键可打开反应堆 UI；Inventory 委托到对应的中心核反应堆。
 */
@ModBlock(name = "reactor_access_hatch", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorAccessHatchBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ReactorAccessHatchBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
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

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        // 查找反应堆，仅当访问接口在其 5×5×5 结构内部时才打开 UI（不根据距离，根据结构归属）
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val reactorPos = pos.add(dx, dy, dz)
                    val be = world.getBlockEntity(reactorPos)
                    if (be is NuclearReactorBlockEntity && be.isPositionInStructure(pos)) {
                        player.openHandledScreen(be)
                        return ActionResult.SUCCESS
                    }
                }
            }
        }
        return ActionResult.PASS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val vessel = ReactorVesselBlock::class.item()
            if (vessel != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorAccessHatchBlock::class.item(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .input('A', vessel)
                    .input('B', Items.OAK_TRAPDOOR)
                    .criterion(hasItem(vessel), conditionsFromItem(vessel))
                    .offerTo(exporter, ReactorAccessHatchBlock::class.id())
            }
        }
    }
}

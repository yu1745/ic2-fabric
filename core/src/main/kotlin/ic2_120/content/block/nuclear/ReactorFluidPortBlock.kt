package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.content.item.EmptyCell
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
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

/**
 * 反应堆流体接口。
 * 提供流体交互功能，通过管道输入冷却液/输出热冷却液。
 */
@ModBlock(name = "reactor_fluid_port", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorFluidPortBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ReactorFluidPortBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, ReactorFluidPortBlockEntity::class.type()){ w, p, s, be ->
            (be as ReactorFluidPortBlockEntity).tick(w, p, s)
        }

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
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val vessel = ReactorVesselBlock::class.item()
            val emptyCell = EmptyCell::class.instance()
            if (vessel != Items.AIR && emptyCell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorFluidPortBlock::class.item(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .input('A', vessel)
                    .input('B', emptyCell)
                    .criterion(hasItem(vessel), conditionsFromItem(vessel))
                    .offerTo(exporter, ReactorFluidPortBlock::class.id())
            }
        }
    }
}

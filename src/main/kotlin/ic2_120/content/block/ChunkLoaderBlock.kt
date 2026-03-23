package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.ChunkLoaderBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.minecraft.block.BlockState
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import ic2_120.registry.id
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
import java.util.function.Consumer

/**
 * 区块加载器。消耗 EU 强制加载周围 1～25 个区块，运行时显示绿色。
 * 能量等级：1 (LV)
 */
@ModBlock(name = "chunk_loader", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class ChunkLoaderBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ChunkLoaderBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ChunkLoaderBlockEntity::class.type()) { w, p, s, be -> (be as ChunkLoaderBlockEntity).tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            (world.getBlockEntity(pos) as? ChunkLoaderBlockEntity)?.releaseChunks()
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            val tinPlate = ic2_120.content.item.TinPlate::class.instance()
            val machine = MachineCasingBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            if (tinPlate != Items.AIR && machine != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ChunkLoaderBlock::class.item(), 1)
                    .pattern("TET").pattern("LML").pattern("TCT")
                    .input('T', tinPlate).input('E', Items.ENDER_PEARL).input('L', Items.LAPIS_LAZULI)
                    .input('M', machine).input('C', circuit)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, ChunkLoaderBlock::class.id())
            }
        }
    }
}

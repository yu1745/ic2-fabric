package ic2_120.content.block

import ic2_120.content.block.machines.MagnetizerBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

@ModBlock(name = "magnetizer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class MagnetizerBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        MagnetizerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MagnetizerBlockEntity::class.type()){ w, p, s, be ->
            (be as MagnetizerBlockEntity).tick(w, p, s)
        }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { player.openHandledScreen(it) }
        }
        return ActionResult.SUCCESS
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE, Properties.POWERED)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)?.with(Properties.POWERED, false)

    override fun neighborUpdate(
        state: BlockState,
        world: World,
        pos: BlockPos,
        sourceBlock: net.minecraft.block.Block,
        sourcePos: BlockPos,
        notify: Boolean
    ) {
        if (!world.isClient) {
            val powered = world.isReceivingRedstonePower(pos)
            if (state.get(Properties.POWERED) != powered) {
                world.setBlockState(pos, state.with(Properties.POWERED, powered))
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val machine = MachineCasingBlock::class.item()
            val ironFence = IronFenceBlock::class.item()
            if (machine != Items.AIR && ironFence != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MagnetizerBlock::class.item(), 1)
                    .pattern("RIR")
                    .pattern("RMR")
                    .pattern("RIR")
                    .input('R', Items.REDSTONE)
                    .input('I', ironFence)
                    .input('M', machine)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, MagnetizerBlock::class.recipeId("default"))
            }
        }
    }
}

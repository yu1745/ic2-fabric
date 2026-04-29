package ic2_120.content.block

import ic2_120.content.block.machines.BlockCutterBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.ElectricMotor
import ic2_120.content.recipes.blockcutter.BlockCutterRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
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
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import ic2_120.registry.annotation.RecipeProvider

/**
 * 方块切割机。将方块锯成 9 个对应板，或增产 50% 木板和木棍。
 * 能量等级 1（LV），最大 32 EU/t，耗能 4 EU/t，切割 1 个方块约 22.5 秒（1800 EU）。
 * 需放置锯片，越硬的锯片可切的东西越多。过压爆炸机制已启用。
 */
@ModBlock(name = "block_cutter", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class BlockCutterBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        BlockCutterBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, BlockCutterBlockEntity::class.type()){ w, p, s, be -> (be as BlockCutterBlockEntity).tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
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

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        /**
         * 为 ClassScanner 生成配方
         */
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val motor = ElectricMotor::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && motor != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BlockCutterBlock::class.item(), 1)
                    .pattern(" c ")
                    .pattern(" M ")
                    .pattern(" e ")
                    .input('c', circuit)
                    .input('M', machine)
                    .input('e', motor)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, BlockCutterBlock::class.id())
            }
            BlockCutterRecipeDatagen.generateRecipes(exporter)
        }
    }
}

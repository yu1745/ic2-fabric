package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.FluidBottlerBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.EmptyCell
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import ic2_120.registry.id
import java.util.function.Consumer

/**
 * 流体装罐机方块。
 * 将满流体容器倒入内部储罐，或将储罐流体灌入空容器。
 * 能量等级：1 (32 EU/t)
 * 操作时间：5 秒/桶，2 EU/t，单次 200 EU
 */
@ModBlock(name = "fluid_bottler", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class FluidBottlerBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        FluidBottlerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, FluidBottlerBlockEntity::class.type()) { w, p, s, be -> (be as FluidBottlerBlockEntity).tick(w, p, s) }

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

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val emptyCell = EmptyCell::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && emptyCell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, FluidBottlerBlock::class.item(), 1)
                    .pattern(" C ").pattern(" C ").pattern("TMT")
                    .input('C', emptyCell).input('T', circuit).input('M', machine)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, FluidBottlerBlock::class.id())
            }
        }
    }
}

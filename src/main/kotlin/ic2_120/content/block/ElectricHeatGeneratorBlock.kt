package ic2_120.content.block

import ic2_120.content.block.machines.ElectricHeatGeneratorBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.HeatConductor
import ic2_120.content.item.IronCasing
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
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
import net.minecraft.data.server.recipe.RecipeJsonProvider
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

@ModBlock(name = "electric_heat_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "heat")
class ElectricHeatGeneratorBlock : MachineBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ElectricHeatGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ElectricHeatGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as ElectricHeatGeneratorBlockEntity).tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState
            .with(Properties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing)
            .with(ACTIVE, false)

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

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val casing = IronCasing::class.instance()
            val battery = ReBatteryItem::class.instance()
            val circuit = Circuit::class.instance()
            val heatConductor = HeatConductor::class.instance()
            if (casing != Items.AIR && battery != Items.AIR && circuit != Items.AIR && heatConductor != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ElectricHeatGeneratorBlock::class.item(), 1)
                    .pattern("CBC")
                    .pattern("CRC")
                    .pattern("CHC")
                    .input('C', casing)
                    .input('B', battery)
                    .input('R', circuit)
                    .input('H', heatConductor)
                    .criterion(hasItem(circuit), conditionsFromItem(circuit))
                    .offerTo(exporter, ElectricHeatGeneratorBlock::class.id())
            }
        }
    }
}

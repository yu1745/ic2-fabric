package ic2_120.content.block

import ic2_120.content.block.machines.WaterGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
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
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

/**
 * 水力发电机方块。
 * 水桶发电 500 EU（1 EU/t），周围 3x3x3 水方块每块 +0.01 EU/t。
 */
@ModBlock(name = "water_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class WaterGeneratorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        WaterGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, WaterGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as WaterGeneratorBlockEntity).tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

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
            val storage = FluidStorage.SIDED.find(world, pos, hit.side)
                ?: FluidStorage.SIDED.find(world, pos, null)
            if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
                return ActionResult.SUCCESS
            }
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
            val generator = GeneratorBlock::class.item()
            if (generator != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WaterGeneratorBlock::class.item(), 2)
                    .pattern("SWS")
                    .pattern("WGW")
                    .pattern("SWS")
                    .input('S', Items.STICK)
                    .input('W', Items.OAK_PLANKS)
                    .input('G', generator)
                    .criterion(hasItem(generator), conditionsFromItem(generator))
                    .offerTo(exporter, WaterGeneratorBlock::class.id())
            }
        }
    }
}

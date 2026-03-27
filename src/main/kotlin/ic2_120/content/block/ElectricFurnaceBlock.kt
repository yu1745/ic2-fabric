package ic2_120.content.block

import ic2_120.content.block.machines.ElectricFurnaceBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import net.minecraft.block.BlockState
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

/**
 * 电炉方块。支持 facing（朝向）与 active（工作中）状态，与 ic2 行为一致。
 * 能量等级：1
 */
@ModBlock(name = "electric_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class ElectricFurnaceBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ElectricFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ElectricFurnaceBlockEntity::class.type()) { world1, pos, state, be -> (be as ElectricFurnaceBlockEntity).tick(world1, pos, state) }

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

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val circuit = Circuit::class.instance()
            val ironFurnace = IronFurnaceBlock::class.item()
            if (circuit != Items.AIR && ironFurnace != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ElectricFurnaceBlock::class.item(), 1)
                    .pattern("   ")
                    .pattern(" c ")
                    .pattern("RFR")
                    .input('c', circuit)
                    .input('R', Items.REDSTONE)
                    .input('F', ironFurnace)
                    .criterion(hasItem(ironFurnace), conditionsFromItem(ironFurnace))
                    .offerTo(exporter, ElectricFurnaceBlock::class.id())
            }
        }
    }
}

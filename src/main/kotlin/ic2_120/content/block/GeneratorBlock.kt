package ic2_120.content.block

import ic2_120.content.block.machines.GeneratorBlockEntity
import ic2_120.content.item.IronPlate
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
import net.minecraft.data.server.recipe.RecipeExporter
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
 * 火力发电机方块。燃烧燃料产生 EU，支持 facing 与 active 状态。
 */
@ModBlock(name = "generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class GeneratorBlock : MachineBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        GeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, GeneratorBlockEntity::class.type()){ w, p, s, be -> (be as GeneratorBlockEntity).tick(w, p, s) }

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

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val battery = ReBatteryItem::class.instance()
            val ironPlate = IronPlate::class.instance()
            val ironFurnace = IronFurnaceBlock::class.item()
            if (battery != Items.AIR && ironPlate != Items.AIR && ironFurnace != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, GeneratorBlock::class.item(), 1)
                    .pattern(" B ")
                    .pattern("III")
                    .pattern(" F ")
                    .input('B', battery)
                    .input('I', ironPlate)
                    .input('F', ironFurnace)
                    .criterion(hasItem(ironPlate), conditionsFromItem(ironPlate))
                    .offerTo(exporter, GeneratorBlock::class.id())
            }
        }
    }
}

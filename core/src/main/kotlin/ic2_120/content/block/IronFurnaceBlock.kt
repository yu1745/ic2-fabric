package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 铁炉方块。使用燃料烧制物品，比原版熔炉快 20%。
 *
 * 特点：
 * - 烧制速度：8秒（原版熔炉10秒）
 * - 燃料效率：煤炭可烧制10个物品（原版8个）
 * - 岩浆效率低：岩浆只能烧制12.5个物品（原版100个）
 * - 工作时不发光
 */
@ModBlock(name = "iron_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class IronFurnaceBlock : MachineBlock() {

    override fun getCasingDrop() = asItem()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        IronFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, IronFurnaceBlockEntity::class.type()){ world1, pos, state, be -> (be as IronFurnaceBlockEntity).tick(world1, pos, state) }

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
        fun generateRecipes(exporter: RecipeExporter) {
            val ironPlate = ic2_120.content.item.IronPlate::class.instance()
            if (ironPlate != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronFurnaceBlock::class.item(), 1)
                    .pattern(" I ").pattern("IFI").pattern("I I")
                    .input('I', ironPlate).input('F', Items.FURNACE)
                    .criterion(hasItem(ironPlate), conditionsFromItem(ironPlate))
                    .offerTo(exporter, IronFurnaceBlock::class.id())
            }
        }
    }
}

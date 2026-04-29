package ic2_120.content.block

import ic2_120.content.block.machines.CannerBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.TinCasing
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
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 流体/固体装罐机方块。
 * 复合流体装罐（满容器倒出/空容器灌入）与固体装罐（锡罐+食物）。
 * 能量等级：1 (32 EU/t)
 * 操作时间：10 秒，4 EU/t，单次 800 EU
 */
@ModBlock(name = "canner", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class CannerBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        CannerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, CannerBlockEntity::class.type()){ w, p, s, be -> (be as CannerBlockEntity).tick(w, p, s) }

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
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val tin = TinCasing::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && tin != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CannerBlock::class.item(), 1)
                    .pattern("TCT")
                    .pattern("TMT")
                    .pattern("TTT")
                    .input('T', tin)
                    .input('C', circuit)
                    .input('M', machine)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, CannerBlock::class.id())
            }
        }
    }
}

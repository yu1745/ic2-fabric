package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.TeslaCoilBlockEntity
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
import net.minecraft.data.server.recipe.RecipeJsonProvider
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
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.recipeId
import java.util.function.Consumer

/**
 * 特斯拉线圈方块。消耗电力对范围内生物释放闪电。
 * 需要红石信号激活，能量等级 2（MV）。
 * 不支持升级。
 */
@ModBlock(name = "tesla_coil", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class TeslaCoilBlock : MachineBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        TeslaCoilBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, TeslaCoilBlockEntity::class.type()) { w, p, s, be ->
            (be as TeslaCoilBlockEntity).tick(w, p, s)
        }

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

        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val redstone = Items.REDSTONE
            val ironCasing = ic2_120.content.item.IronCasing::class.instance()
            val steelIngot = ic2_120.content.item.SteelIngot::class.instance()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            val centerItem = if (MvTransformerBlock::class.item() != Items.AIR) MvTransformerBlock::class.item() else MachineBlock::class.item()

            // 配方一：5 红石粉 + 1 中压变压器 + 2 铁质外壳 + 1 电路板
            if (ironCasing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, TeslaCoilBlock::class.item(), 1)
                    .pattern("RRR").pattern("RTR").pattern("ICI")
                    .input('R', redstone).input('T', centerItem).input('I', ironCasing).input('C', circuit)
                    .criterion(hasItem(ironCasing), conditionsFromItem(ironCasing))
                    .offerTo(exporter, TeslaCoilBlock::class.recipeId("iron"))
            }

            // 配方二：5 红石粉 + 1 中压变压器 + 2 钢锭 + 1 电路板
            if (steelIngot != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, TeslaCoilBlock::class.item(), 1)
                    .pattern("RRR").pattern("RTR").pattern("SCS")
                    .input('R', redstone).input('T', centerItem).input('S', steelIngot).input('C', circuit)
                    .criterion(hasItem(steelIngot), conditionsFromItem(steelIngot))
                    .offerTo(exporter, TeslaCoilBlock::class.recipeId("steel"))
            }
        }
    }
}

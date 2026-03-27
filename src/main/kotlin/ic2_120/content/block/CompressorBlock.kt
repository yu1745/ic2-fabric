package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.CompressorBlockEntity
import ic2_120.content.recipes.compressor.CompressorRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
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
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 压缩机方块。消耗电力将物品压缩。
 * 能量等级：1
 */
@ModBlock(name = "compressor", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class CompressorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        CompressorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, CompressorBlockEntity::class.type()) { w, p, s, be -> (be as CompressorBlockEntity).tick(w, p, s) }

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
            // 机器本身合成配方
            val machine = MachineCasingBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CompressorBlock::class.item(), 1)
                    .pattern("SSS").pattern("SMS").pattern("SCS")
                    .input('S', Items.STONE).input('M', machine).input('C', circuit)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, CompressorBlock::class.id())
            }

            // 机器配方
            CompressorRecipeDatagen.generateRecipes(exporter)
        }
    }
}

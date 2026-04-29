package ic2_120.content.block

import ic2_120.content.block.machines.BlastFurnaceBlockEntity
import ic2_120.content.item.HeatConductor
import ic2_120.content.item.IronCasing
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import ic2_120.registry.annotation.RecipeProvider

/**
 * 高炉方块。
 * 从背面接收热量（HU），消耗铁质材料与压缩空气，产出钢锭和炉渣。
 */
@ModBlock(name = "blast_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class BlastFurnaceBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        BlastFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, BlastFurnaceBlockEntity::class.type()){ w, p, s, be -> (be as BlastFurnaceBlockEntity).tick(w, p, s) }

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

    override fun getPlacementState(ctx: net.minecraft.item.ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        /**
         * 为 ClassScanner 生成配方
         */
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            BlastFurnaceRecipeDatagen.generateRecipes(exporter)
            val ironCasing = IronCasing::class.instance()
            val machineCasing = MachineCasingBlock::class.item()
            val heatConductor = HeatConductor::class.instance()
            if (ironCasing != Items.AIR && machineCasing != Items.AIR && heatConductor != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BlastFurnaceBlock::class.item(), 1)
                    .pattern("CCC")
                    .pattern("CMC")
                    .pattern("CHC")
                    .input('C', ironCasing)
                    .input('M', machineCasing)
                    .input('H', heatConductor)
                    .criterion(hasItem(machineCasing), conditionsFromItem(machineCasing))
                    .offerTo(exporter, BlastFurnaceBlock::class.id())
            }
        }
    }
}

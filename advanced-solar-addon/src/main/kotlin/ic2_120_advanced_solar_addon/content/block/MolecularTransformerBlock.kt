package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.item.MtCore
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.AdvancedMachineCasingBlock
import ic2_120.content.block.HvTransformerBlock
import ic2_120.content.item.AdvancedCircuit
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockRenderType
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
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

// i18n: block.ic2_120_advanced_solar_addon.molecular_transformer
// zh_cn: 分子重组仪
// en_us: Molecular Transformer
@ModBlock(name = "molecular_transformer", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "machine")
class MolecularTransformerBlock : MachineBlock() {

    companion object {
        const val INPUT_SLOT = 0
        const val OUTPUT_SLOT = 1
        const val INVENTORY_SIZE = 2
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val mtCore = MtCore::class.instance()
            val casing = AdvancedMachineCasingBlock::class.item()
            val transformer = HvTransformerBlock::class.item()
            val circuit = AdvancedCircuit::class.instance()

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MolecularTransformerBlock::class.item(), 1)
                .pattern("CTC")
                .pattern("CMC")
                .pattern("CTC")
                .input('C', casing)
                .input('T', transformer)
                .input('M', mtCore)
                .criterion(hasItem(circuit), conditionsFromItem(circuit))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("molecular_transformer"))
        }
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        MolecularTransformerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MolecularTransformerBlockEntity::class.type()) { w: World, p: BlockPos, s: BlockState, be -> (be as MolecularTransformerBlockEntity).tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? NamedScreenHandlerFactory
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

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.INVISIBLE

    // BER model is smaller than a full cube, so this block must not occlude neighbor faces.
    override fun getCullingShape(state: BlockState, world: BlockView, pos: BlockPos): VoxelShape =
        VoxelShapes.empty()
}

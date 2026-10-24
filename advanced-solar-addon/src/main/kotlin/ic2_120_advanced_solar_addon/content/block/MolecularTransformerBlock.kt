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
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.MenuProvider
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import java.util.function.Consumer

// i18n: block.ic2_120_advanced_solar_addon.molecular_transformer
// zh_cn: 分子重组仪
// en_us: Molecular Transformer
@ModBlock(name = "molecular_transformer", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "machine")
class MolecularTransformerBlock : MachineBlock(
    defaultMachineSettings().luminance { state -> if (state.get(LIT)) 7 else 0 }
) {

    companion object {
        const val INPUT_SLOT = 0
        const val OUTPUT_SLOT = 1
        const val INVENTORY_SIZE = 2
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
        val LIT: BooleanProperty = BooleanProperty.of("lit")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val mtCore = MtCore::class.instance()
            val casing = AdvancedMachineCasingBlock::class.item()
            val transformer = HvTransformerBlock::class.item()
            val circuit = AdvancedCircuit::class.instance()

            ShapedRecipeBuilder.create(RecipeCategory.MISC, MolecularTransformerBlock::class.item(), 1)
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
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, MolecularTransformerBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? MenuProvider
    }

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE, LIT)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)?.with(LIT, false)

    override fun getRenderType(state: BlockState): RenderShape = RenderShape.INVISIBLE

    // BER model is smaller than a full cube, so this block must not occlude neighbor faces.
    @Deprecated("override deprecated member", level = DeprecationLevel.WARNING)
    override fun getCullingShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape =
        Shapes.empty()
}

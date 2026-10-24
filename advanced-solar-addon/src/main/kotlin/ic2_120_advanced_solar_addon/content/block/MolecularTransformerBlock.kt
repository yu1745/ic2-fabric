package ic2_120_advanced_solar_addon.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

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
import ic2_120.compat.*
import net.minecraft.data.recipes.RecipeProvider.has

// i18n: block.ic2_120_advanced_solar_addon.molecular_transformer
// zh_cn: 分子重组仪
// en_us: Molecular Transformer
@ModBlock(name = "molecular_transformer", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "machine")
class MolecularTransformerBlock : MachineBlock(
    defaultMachineSettings().lightLevel { state -> if (state.getValue(LIT)) 7 else 0 }
) {

    companion object {
        const val INPUT_SLOT = 0
        const val OUTPUT_SLOT = 1
        const val INVENTORY_SIZE = 2
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")
        val LIT: BooleanProperty = BooleanProperty.create("lit")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val mtCore = MtCore::class.instance()
            val casing = AdvancedMachineCasingBlock::class.item()
            val transformer = HvTransformerBlock::class.item()
            val circuit = AdvancedCircuit::class.instance()

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MolecularTransformerBlock::class.item(), 1)
                .pattern("CTC")
                .pattern("CMC")
                .pattern("CTC")
                .define('C', casing)
                .define('T', transformer)
                .define('M', mtCore)
                .unlockedBy(getHasName(circuit), has(circuit))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("molecular_transformer"))
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        MolecularTransformerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MolecularTransformerBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE, LIT)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)?.setValue(LIT, false)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE

}

package ic2_120.content.block

import ic2_120.content.block.cables.GlassFibreCableBlock
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.FrequencyTransmitter
import ic2_120.content.item.RecallScrollItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.id
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
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
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

@ModBlock(name = "teleporter", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class TeleporterBlock : MachineBlock() {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        TeleporterBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, TeleporterBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): net.minecraft.world.MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.world.MenuProvider
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val stack = player.getStackInHand(hand)
        if (stack.item is FrequencyTransmitter || stack.item is RecallScrollItem) {
            return InteractionResult.PASS
        }

        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val advancedMachine = AdvancedMachineCasingBlock::class.item()
            val freq = FrequencyTransmitter::class.instance()
            val advancedCircuit = AdvancedCircuit::class.instance()
            val glassFibre = GlassFibreCableBlock::class.item()

            if (advancedMachine != Items.AIR && freq != Items.AIR && advancedCircuit != Items.AIR && glassFibre != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, TeleporterBlock::class.item(), 1)
                    .pattern("afa")
                    .pattern("gmg")
                    .pattern("ada")
                    .input('a', advancedCircuit)
                    .input('f', freq)
                    .input('g', glassFibre)
                    .input('m', advancedMachine)
                    .input('d', Items.DIAMOND)
                    .criterion(hasItem(advancedMachine), conditionsFromItem(advancedMachine))
                    .offerTo(exporter, TeleporterBlock::class.id())
            }
        }
    }
}

package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

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
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
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
import ic2_120.registry.annotation.RecipeProvider

@ModBlock(name = "teleporter", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class TeleporterBlock : MachineBlock() {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        TeleporterBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, TeleporterBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        val stack = player.mainHandItem
        if (stack.item is FrequencyTransmitter || stack.item is RecallScrollItem) {
            return InteractionResult.PASS
        }

        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val advancedMachine = AdvancedMachineCasingBlock::class.item()
            val freq = FrequencyTransmitter::class.instance()
            val advancedCircuit = AdvancedCircuit::class.instance()
            val glassFibre = GlassFibreCableBlock::class.item()

            if (advancedMachine != Items.AIR && freq != Items.AIR && advancedCircuit != Items.AIR && glassFibre != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TeleporterBlock::class.item(), 1)
                    .pattern("afa")
                    .pattern("gmg")
                    .pattern("ada")
                    .define('a', advancedCircuit)
                    .define('f', freq)
                    .define('g', glassFibre)
                    .define('m', advancedMachine)
                    .define('d', Items.DIAMOND)
                    .unlockedBy(getHasName(advancedMachine), has(advancedMachine))
                    .save(exporter, TeleporterBlock::class.id())
            }
        }
    }
}

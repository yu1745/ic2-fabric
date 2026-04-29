package ic2_120.content.block

import ic2_120.content.block.machines.MatterGeneratorBlockEntity
import ic2_120.content.item.AdvancedCircuit
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
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
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlock(name = "matter_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "uu")
class MatterGeneratorBlock : MachineBlock() {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        MatterGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MatterGeneratorBlockEntity::class.type()){ w, p, s, be ->
            (be as MatterGeneratorBlockEntity).tick(w, p, s)
        }

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: World,
        pos: BlockPos
    ): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
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
            val advancedMachine = AdvancedMachineCasingBlock::class.item()
            val advancedCircuit = AdvancedCircuit::class.instance()
            val lapotronCrystal = LapotronCrystalItem::class.instance()
            if (advancedMachine != Items.AIR && advancedCircuit != Items.AIR && lapotronCrystal != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MatterGeneratorBlock::class.item(), 1)
                    .pattern("GCG")
                    .pattern("AMA")
                    .pattern("GCG")
                    .input('G', Items.GLOWSTONE)
                    .input('C', advancedCircuit)
                    .input('A', advancedMachine)
                    .input('M', lapotronCrystal)
                    .criterion(hasItem(advancedMachine), conditionsFromItem(advancedMachine))
                    .offerTo(exporter, MatterGeneratorBlock::class.id())
            }
        }
    }
}

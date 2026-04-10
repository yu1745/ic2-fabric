package ic2_120.content.block

import ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity
import ic2_120.content.item.ToolHandleIronItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

@ModBlock(name = "water_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class WaterKineticGeneratorBlock : MachineBlock() {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WaterKineticGeneratorBlock::class.item(), 1)
                .pattern("   ")
                .pattern("SCS")
                .pattern("   ")
                .input('S', ToolHandleIronItem::class.instance())
                .input('C', MachineCasingBlock::class.instance())
                .criterion(
                    hasItem(MachineCasingBlock::class.instance()),
                    conditionsFromItem(MachineCasingBlock::class.instance())
                )
                .offerTo(exporter, WaterKineticGeneratorBlock::class.id())
        }
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: net.minecraft.item.ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        WaterKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, WaterKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as WaterKineticGeneratorBlockEntity).tick(w, p, s)
        }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        val be = world.getBlockEntity(pos) as? WaterKineticGeneratorBlockEntity ?: return ActionResult.PASS
        if (world.isClient) return ActionResult.SUCCESS

        val interaction = be.onUse(player, hand)
        if (interaction != ActionResult.PASS) return interaction

        player.openHandledScreen(be)
        return ActionResult.SUCCESS
    }
}
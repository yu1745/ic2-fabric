package ic2_120.content.block

import ic2_120.content.block.machines.ReplicatorBlockEntity
import ic2_120.content.block.MfeBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
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

@ModBlock(name = "replicator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "uu")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class ReplicatorBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ReplicatorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, ReplicatorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun createScreenHandlerFactory(
        state: BlockState,
        world: Level,
        pos: BlockPos
    ): net.minecraft.world.MenuProvider? =
        world.getBlockEntity(pos) as? net.minecraft.world.MenuProvider

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let(player::openHandledScreen)
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
            val reinforcedStone = ReinforcedStoneBlock::class.item()
            val teleporter = TeleporterBlock::class.item()
            val hvTransformer = HvTransformerBlock::class.item()
            val mfe = MfeBlock::class.item()
            if (advancedMachine != Items.AIR && reinforcedStone != Items.AIR && teleporter != Items.AIR && hvTransformer != Items.AIR && mfe != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, ReplicatorBlock::class.item(), 1)
                    .pattern("SAS")
                    .pattern("TTT")
                    .pattern("HMH")
                    .input('S', reinforcedStone)
                    .input('A', advancedMachine)
                    .input('T', teleporter)
                    .input('H', hvTransformer)
                    .input('M', mfe)
                    .criterion(hasItem(teleporter), conditionsFromItem(teleporter))
                    .offerTo(exporter, ReplicatorBlock::class.id())
            }
        }
    }
}

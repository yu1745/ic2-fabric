package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.ReplicatorBlockEntity
import ic2_120.content.block.MfeBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
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
import java.util.function.Consumer

@ModBlock(name = "replicator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "uu")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class ReplicatorBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ReplicatorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, ReplicatorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun getMenuProvider(
        state: BlockState,
        world: Level,
        pos: BlockPos
    ): net.minecraft.world.MenuProvider? =
        world.getBlockEntity(pos) as? net.minecraft.world.MenuProvider

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let(player::openMenu)
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
            val reinforcedStone = ReinforcedStoneBlock::class.item()
            val teleporter = TeleporterBlock::class.item()
            val hvTransformer = HvTransformerBlock::class.item()
            val mfe = MfeBlock::class.item()
            if (advancedMachine != Items.AIR && reinforcedStone != Items.AIR && teleporter != Items.AIR && hvTransformer != Items.AIR && mfe != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ReplicatorBlock::class.item(), 1)
                    .pattern("SAS")
                    .pattern("TTT")
                    .pattern("HMH")
                    .define('S', reinforcedStone)
                    .define('A', advancedMachine)
                    .define('T', teleporter)
                    .define('H', hvTransformer)
                    .define('M', mfe)
                    .unlockedBy(getHasName(teleporter), has(teleporter))
                    .save(exporter, ReplicatorBlock::class.id())
            }
        }
    }
}

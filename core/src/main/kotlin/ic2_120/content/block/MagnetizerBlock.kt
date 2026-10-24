package ic2_120.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.MagnetizerBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.recipeId
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

@ModBlock(name = "magnetizer", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class MagnetizerBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        MagnetizerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, MagnetizerBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { player.openMenu(it) }
        }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE, BlockStateProperties.POWERED)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)?.setValue(BlockStateProperties.POWERED, false)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun neighborUpdate(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: net.minecraft.world.level.block.Block,
        sourcePos: BlockPos,
        notify: Boolean
    ) {
        if (!world.isClient) {
            val powered = world.isReceivingRedstonePower(pos)
            if (state.getValue(BlockStateProperties.POWERED) != powered) {
                world.setBlock(pos, state.setValue(BlockStateProperties.POWERED, powered))
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val machine = MachineCasingBlock::class.item()
            val ironFence = IronFenceBlock::class.item()
            if (machine != Items.AIR && ironFence != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MagnetizerBlock::class.item(), 1)
                    .pattern("RIR")
                    .pattern("RMR")
                    .pattern("RIR")
                    .define('R', Items.REDSTONE)
                    .define('I', ironFence)
                    .define('M', machine)
                    .unlockedBy(getHasName(machine), has(machine))
                    .save(exporter, MagnetizerBlock::class.recipeId("default"))
            }
        }
    }
}

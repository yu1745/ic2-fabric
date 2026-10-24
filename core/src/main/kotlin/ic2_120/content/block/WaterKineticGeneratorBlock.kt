package ic2_120.content.block

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity
import ic2_120.content.item.ToolHandleIronItem
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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.Level
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer

@ModBlock(name = "water_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class WaterKineticGeneratorBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, WaterKineticGeneratorBlock::class.item(), 1)
                .pattern("S S")
                .pattern(" C ")
                .pattern("S S")
                .define('S', ToolHandleIronItem::class.instance())
                .define('C', MachineCasingBlock::class.instance())
                .unlockedBy(
                    has(MachineCasingBlock::class.instance()),
                    has(MachineCasingBlock::class.instance())
                )
                .save(exporter, WaterKineticGeneratorBlock::class.id())
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        WaterKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, WaterKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        val be = world.getBlockEntity(pos) as? WaterKineticGeneratorBlockEntity ?: return InteractionResult.PASS
        if (world.isClient) return InteractionResult.SUCCESS

        val interaction = be.onUse(player, hand)
        if (interaction != InteractionResult.PASS) return interaction

        player.openMenu(be)
        return InteractionResult.SUCCESS
    }
}
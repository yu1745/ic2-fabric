package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.getHasName
import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.ManualKineticGeneratorBlockEntity
import ic2_120.content.item.CrankHandleItem
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
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.Level
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory

@ModBlock(name = "manual_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class ManualKineticGeneratorBlock : DirectionalMachineBlock() {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val casing = MachineCasingBlock::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ManualKineticGeneratorBlock::class.item(), 1)
                .pattern("L")
                .pattern("C")
                .define('L', net.minecraft.world.item.Items.LEVER)
                .define('C', casing)
                .unlockedBy(
                    getHasName(casing),
                    has(casing)
                )
                .save(exporter, ManualKineticGeneratorBlock::class.id())
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? {
        // 允许点击任意面放置，但方块朝向始终为水平方向（玩家面朝的方向）
        return defaultBlockState().setValue(BlockStateProperties.FACING, ctx.horizontalDirection).setValue(ACTIVE, false)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ManualKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        validateTicker(type, ManualKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        val be = world.getBlockEntity(pos) as? ManualKineticGeneratorBlockEntity ?: return InteractionResult.PASS

        // 只有顶部（UP 面）可以插入/转动曲柄
        if (hit.direction != Direction.UP) {
            return InteractionResult.PASS
        }

        return be.onUse(player, InteractionHand.MAIN_HAND, world, pos, state)
    }

    override fun playerWillDestroy(world: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? ManualKineticGeneratorBlockEntity
            be?.dropCrank()
        }
        return super.playerWillDestroy(world, pos, state, player)
    }
}

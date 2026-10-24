package ic2_120.content.block

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
import net.minecraft.world.level.block.entity.BlockEntityType
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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer

@ModBlock(name = "manual_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class ManualKineticGeneratorBlock : DirectionalMachineBlock() {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val casing = MachineCasingBlock::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, ManualKineticGeneratorBlock::class.item(), 1)
                .pattern("L")
                .pattern("C")
                .input('L', net.minecraft.world.item.Items.LEVER)
                .input('C', casing)
                .criterion(
                    hasItem(casing),
                    conditionsFromItem(casing)
                )
                .offerTo(exporter, ManualKineticGeneratorBlock::class.id())
        }
    }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? {
        // 允许点击任意面放置，但方块朝向始终为水平方向（玩家面朝的方向）
        return defaultState.with(BlockStateProperties.FACING, ctx.horizontalPlayerFacing).with(ACTIVE, false)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ManualKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        validateTicker(world, type, ManualKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
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
        val be = world.getBlockEntity(pos) as? ManualKineticGeneratorBlockEntity ?: return InteractionResult.PASS

        // 只有顶部（UP 面）可以插入/转动曲柄
        if (hit.side != Direction.UP) {
            return InteractionResult.PASS
        }

        return be.onUse(player, hand, world, pos, state)
    }

    override fun onBreak(world: Level, pos: BlockPos, state: BlockState, player: Player) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? ManualKineticGeneratorBlockEntity
            be?.dropCrank()
        }
        super.onBreak(world, pos, state, player)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onEntityCollision(state: BlockState, world: Level, pos: BlockPos, entity: net.minecraft.world.entity.Entity) {
        // 不处理碰撞
    }
}

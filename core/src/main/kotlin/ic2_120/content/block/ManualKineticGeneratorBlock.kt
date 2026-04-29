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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
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

@ModBlock(name = "manual_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class ManualKineticGeneratorBlock : DirectionalMachineBlock() {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val casing = MachineCasingBlock::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ManualKineticGeneratorBlock::class.item(), 1)
                .pattern("L")
                .pattern("C")
                .input('L', net.minecraft.item.Items.LEVER)
                .input('C', casing)
                .criterion(
                    hasItem(casing),
                    conditionsFromItem(casing)
                )
                .offerTo(exporter, ManualKineticGeneratorBlock::class.id())
        }
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: net.minecraft.item.ItemPlacementContext): BlockState? {
        // 允许点击任意面放置，但方块朝向始终为水平方向（玩家面朝的方向）
        return defaultState.with(Properties.FACING, ctx.horizontalPlayerFacing).with(ACTIVE, false)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ManualKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        checkType(type, ManualKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as ManualKineticGeneratorBlockEntity).tick(w, p, s)
        }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        val be = world.getBlockEntity(pos) as? ManualKineticGeneratorBlockEntity ?: return ActionResult.PASS

        // 只有顶部（UP 面）可以插入/转动曲柄
        if (hit.side != Direction.UP) {
            return ActionResult.PASS
        }

        return be.onUse(player, hand, world, pos, state)
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? ManualKineticGeneratorBlockEntity
            be?.dropCrank()
        }
        super.onBreak(world, pos, state, player)
    }

    override fun onEntityCollision(state: BlockState, world: World, pos: BlockPos, entity: net.minecraft.entity.Entity) {
        // 不处理碰撞
    }
}

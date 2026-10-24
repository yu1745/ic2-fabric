package ic2_120.content.block

import ic2_120.content.block.machines.BlockCutterBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.ElectricMotor
import ic2_120.content.recipes.blockcutter.BlockCutterRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
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
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 方块切割机。将方块锯成 9 个对应板，或增产 50% 木板和木棍。
 * 能量等级 1（LV），最大 32 EU/t，耗能 4 EU/t，切割 1 个方块约 22.5 秒（1800 EU）。
 * 需放置锯片，越硬的锯片可切的东西越多。过压爆炸机制已启用。
 */
@ModBlock(name = "block_cutter", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class BlockCutterBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        BlockCutterBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, BlockCutterBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): net.minecraft.world.MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.world.MenuProvider
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
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
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

        /**
         * 为 ClassScanner 生成配方
         */
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val motor = ElectricMotor::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && motor != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, BlockCutterBlock::class.item(), 1)
                    .pattern(" c ")
                    .pattern(" M ")
                    .pattern(" e ")
                    .input('c', circuit)
                    .input('M', machine)
                    .input('e', motor)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, BlockCutterBlock::class.id())
            }
            BlockCutterRecipeDatagen.generateRecipes(exporter)
        }
    }
}

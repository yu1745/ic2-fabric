package ic2_120.content.block

import ic2_120.content.block.machines.TeslaCoilBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.IronCasing
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 特斯拉线圈方块。消耗电力对范围内生物释放闪电。
 * 需要红石信号激活，能量等级 2（MV）。
 * 不支持升级。
 */
@ModBlock(name = "tesla_coil", registerItem = true, tab = CreativeTab.IC2_MACHINES)
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class TeslaCoilBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        TeslaCoilBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, TeslaCoilBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun appendProperties(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE, BlockStateProperties.POWERED)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)?.with(BlockStateProperties.POWERED, false)

    // BlockBehaviour.neighborUpdate 在 1.20.1 标记为 @Deprecated（Mojang 设计：override 是预期用法，详见其 javadoc）。
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
            if (state.get(BlockStateProperties.POWERED) != powered) {
                world.setBlockState(pos, state.with(BlockStateProperties.POWERED, powered))
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val mv = MvTransformerBlock::class.item()
            val ironCasing = IronCasing::class.instance()
            val circuit = Circuit::class.instance()
            if (mv != Items.AIR && ironCasing != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, TeslaCoilBlock::class.item(), 1)
                    .pattern("RRR")
                    .pattern("RMR")
                    .pattern("ICI")
                    .input('R', Items.REDSTONE)
                    .input('M', mv)
                    .input('I', ironCasing)
                    .input('C', circuit)
                    .criterion(hasItem(mv), conditionsFromItem(mv))
                    .offerTo(exporter, TeslaCoilBlock::class.id())
            }
        }
    }
}

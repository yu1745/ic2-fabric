package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.SolidCannerBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.EmptyTinCanItem
import ic2_120.content.recipes.solidcanner.SolidCannerRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 固体装罐机方块。
 * 将空锡罐与食物装罐成罐装食物。
 * 能量等级：1 (32 EU/t)
 * 操作时间：10 秒，2 EU/t，单次 400 EU
 */
@ModBlock(name = "solid_canner", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class SolidCannerBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        SolidCannerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, SolidCannerBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

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

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 固体装罐机机器配方
            SolidCannerRecipeDatagen.generateRecipes(exporter)

            // 机器合成表
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val tinCan = EmptyTinCanItem::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && tinCan != Items.AIR) {
                // 标准配方：2 空锡罐 + 2 电路板 + 1 基础机械外壳
                ShapedRecipeBuilder.create(RecipeCategory.MISC, SolidCannerBlock::class.item(), 1)
                    .pattern(" C ").pattern(" C ").pattern("TMT")
                    .input('C', tinCan).input('T', circuit).input('M', machine)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, SolidCannerBlock::class.id())
            }
            // if (machine != Items.AIR && circuit != Items.AIR && tinIngot != Items.AIR) {
            //     // LF 配方：7 锡锭 + 1 电路板 + 1 基础机械外壳
            //     ShapedRecipeBuilder.create(RecipeCategory.MISC, SolidCannerBlock::class.item(), 1)
            //         .pattern("TCT").pattern("TMT").pattern("TTT")
            //         .input('T', tinIngot).input('C', circuit).input('M', machine)
            //         .criterion(hasItem(machine), conditionsFromItem(machine))
            //         .offerTo(exporter, ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "solid_canner_lf"))
            // }
        }
    }
}

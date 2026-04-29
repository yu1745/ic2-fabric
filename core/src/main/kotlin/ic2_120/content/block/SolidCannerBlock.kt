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
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
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
class SolidCannerBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        SolidCannerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, SolidCannerBlockEntity::class.type()) { w, p, s, be -> (be as SolidCannerBlockEntity).tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 固体装罐机机器配方
            SolidCannerRecipeDatagen.generateRecipes(exporter)

            // 机器合成表
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val tinCan = EmptyTinCanItem::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && tinCan != Items.AIR) {
                // 标准配方：2 空锡罐 + 2 电路板 + 1 基础机械外壳
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SolidCannerBlock::class.item(), 1)
                    .pattern(" C ").pattern(" C ").pattern("TMT")
                    .input('C', tinCan).input('T', circuit).input('M', machine)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, SolidCannerBlock::class.id())
            }
            // if (machine != Items.AIR && circuit != Items.AIR && tinIngot != Items.AIR) {
            //     // LF 配方：7 锡锭 + 1 电路板 + 1 基础机械外壳
            //     ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SolidCannerBlock::class.item(), 1)
            //         .pattern("TCT").pattern("TMT").pattern("TTT")
            //         .input('T', tinIngot).input('C', circuit).input('M', machine)
            //         .criterion(hasItem(machine), conditionsFromItem(machine))
            //         .offerTo(exporter, Identifier(Ic2_120.MOD_ID, "solid_canner_lf"))
            // }
        }
    }
}

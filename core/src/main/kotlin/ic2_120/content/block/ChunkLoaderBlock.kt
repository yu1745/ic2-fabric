package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.ChunkLoaderBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import ic2_120.registry.id
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
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 区块加载器。消耗 EU 强制加载周围 1～25 个区块，运行时显示绿色。
 * 能量等级：1 (LV)
 */
@ModBlock(name = "chunk_loader", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class ChunkLoaderBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ChunkLoaderBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, ChunkLoaderBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): net.minecraft.world.MenuProvider? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.world.MenuProvider
    }

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
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

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            (world.getBlockEntity(pos) as? ChunkLoaderBlockEntity)?.releaseChunks()
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.recipes.RecipeOutput>) {
            val tinPlate = ic2_120.content.item.TinPlate::class.instance()
            val machine = MachineCasingBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            if (tinPlate != Items.AIR && machine != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, ChunkLoaderBlock::class.item(), 1)
                    .pattern("TET").pattern("LML").pattern("TCT")
                    .input('T', tinPlate).input('E', Items.ENDER_PEARL).input('L', Items.LAPIS_LAZULI)
                    .input('M', machine).input('C', circuit)
                    .criterion(hasItem(machine), conditionsFromItem(machine))
                    .offerTo(exporter, ChunkLoaderBlock::class.id())
            }
        }
    }
}

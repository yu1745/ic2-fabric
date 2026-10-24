package ic2_120.content.block

import ic2_120.content.block.machines.BlastFurnaceBlockEntity
import ic2_120.content.item.HeatConductor
import ic2_120.content.item.IronCasing
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 高炉方块。
 * 从背面接收热量（HU），消耗铁质材料与压缩空气，产出钢锭和炉渣。
 */
@ModBlock(name = "blast_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class BlastFurnaceBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        BlastFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, BlastFurnaceBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

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

    override fun getPlacementState(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        /**
         * 为 ClassScanner 生成配方
         */
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            BlastFurnaceRecipeDatagen.generateRecipes(exporter)
            val ironCasing = IronCasing::class.instance()
            val machineCasing = MachineCasingBlock::class.item()
            val heatConductor = HeatConductor::class.instance()
            if (ironCasing != Items.AIR && machineCasing != Items.AIR && heatConductor != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, BlastFurnaceBlock::class.item(), 1)
                    .pattern("CCC")
                    .pattern("CMC")
                    .pattern("CHC")
                    .input('C', ironCasing)
                    .input('M', machineCasing)
                    .input('H', heatConductor)
                    .criterion(hasItem(machineCasing), conditionsFromItem(machineCasing))
                    .offerTo(exporter, BlastFurnaceBlock::class.id())
            }
        }
    }
}

package ic2_120.content.block

import ic2_120.content.block.machines.InductionFurnaceBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 感应炉方块。使用电力加热并快速烧制物品，支持双槽同时加工。
 * 热量机制：持续红石信号时热量上升（消耗 1 EU/t），无信号时热量衰减。
 * 能量等级：2（MV, 最高 128 EU/t）
 */
@ModBlock(name = "induction_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
class InductionFurnaceBlock : MachineBlock() {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        InductionFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, InductionFurnaceBlockEntity::class.type()){ w, p, s, be -> (be as InductionFurnaceBlockEntity).tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
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

    override fun getPlacementState(ctx: net.minecraft.item.ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val electricFurnace = ElectricFurnaceBlock::class.item()
            val advCasing = AdvancedMachineCasingBlock::class.item()
            if (electricFurnace != Items.AIR && advCasing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, InductionFurnaceBlock::class.item(), 1)
                    .pattern("CCC")
                    .pattern("CEC")
                    .pattern("CAC")
                    .input('C', Items.COPPER_INGOT)
                    .input('E', electricFurnace)
                    .input('A', advCasing)
                    .criterion(hasItem(electricFurnace), conditionsFromItem(electricFurnace))
                    .offerTo(exporter, InductionFurnaceBlock::class.id())
            }
        }
    }
}

package ic2_120.content.block

import ic2_120.Ic2_120
import ic2_120.content.block.machines.RtGeneratorBlockEntity
import ic2_120.content.block.nuclear.ReactorChamberBlock
import ic2_120.content.item.IronCasing
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

/**
 * 放射性同位素温差发电机。
 * 需放入放射性同位素燃料靶丸（rtg_pellet），靶丸无限耐久不消耗。
 * 发电量由靶丸数量决定：1→1, 2→2, 3→4, 4→8, 5→16, 6→32 EU/t
 */
@ModBlock(name = "rt_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class RtGeneratorBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    init {
        defaultState = stateManager.defaultState.with(ACTIVE, false)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        RtGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
<<<<<<< HEAD
        else checkType(type, RtGeneratorBlockEntity::class.type()) { w, p, s, be ->
=======
        else validateTicker(type, RtGeneratorBlockEntity::class.type()){ w, p, s, be ->
>>>>>>> backup/1.21.1
            be.tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

<<<<<<< HEAD
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
=======
    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hit: BlockHitResult): ActionResult {
>>>>>>> backup/1.21.1
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val generator = GeneratorBlock::class.item()
            val ironCasing = IronCasing::class.instance()
            val reactorChamber = ReactorChamberBlock::class.item()
            if (generator != Items.AIR && ironCasing != Items.AIR && reactorChamber != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RtGeneratorBlock::class.item(), 1)
                    .pattern("CCC")
                    .pattern("CRC")
                    .pattern("CGC")
                    .input('C', ironCasing)
                    .input('R', reactorChamber)
                    .input('G', generator)
                    .criterion(hasItem(generator), conditionsFromItem(generator))
                    .offerTo(exporter, RtGeneratorBlock::class.id())
            }
        }
    }
}

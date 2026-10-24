package ic2_120.content.block

import ic2_120.content.block.machines.CondenserBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
<<<<<<< HEAD
import net.minecraft.data.server.recipe.RecipeJsonProvider
=======
import net.minecraft.data.server.recipe.RecipeExporter
>>>>>>> backup/1.21.1
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
<<<<<<< HEAD
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

@ModBlock(name = "condenser", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "machine")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class CondenserBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {
=======
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlock(name = "condenser", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "machine")
class CondenserBlock : MachineBlock() {
>>>>>>> backup/1.21.1

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CondenserBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
<<<<<<< HEAD
        else checkType(type, CondenserBlockEntity::class.type()) { w, p, s, be ->
=======
        else validateTicker(type, CondenserBlockEntity::class.type()) { w, p, s, be ->
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

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
<<<<<<< HEAD
        hand: Hand,
=======
>>>>>>> backup/1.21.1
        hit: BlockHitResult
    ): ActionResult {
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
<<<<<<< HEAD
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
=======
        fun generateRecipes(exporter: RecipeExporter) {
>>>>>>> backup/1.21.1
            val ironCasing = ic2_120.content.item.IronCasing::class.instance()
            val heatVent = ic2_120.content.item.HeatVentItem::class.instance()
            val ironIngot = net.minecraft.item.Items.IRON_INGOT
            if (ironCasing != net.minecraft.item.Items.AIR && heatVent != net.minecraft.item.Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CondenserBlock::class.item(), 1)
                    .pattern("CHC")
                    .pattern("CIC")
                    .pattern("CCC")
                    .input('C', ironCasing)
                    .input('H', heatVent)
                    .input('I', ironIngot)
                    .criterion(hasItem(ironCasing), conditionsFromItem(ironCasing))
                    .offerTo(exporter, CondenserBlock::class.id())
            }
        }
    }
}

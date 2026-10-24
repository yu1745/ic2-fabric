package ic2_120.content.block


import ic2_120.Ic2_120
import ic2_120.content.block.machines.IronFurnaceBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 铁炉方块。使用燃料烧制物品，比原版熔炉快 20%。
 *
 * 特点：
 * - 烧制速度：8秒（原版熔炉10秒）
 * - 燃料效率：煤炭可烧制10个物品（原版8个）
 * - 岩浆效率低：岩浆只能烧制12.5个物品（原版100个）
 * - 工作时不发光
 */
@ModBlock(name = "iron_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class IronFurnaceBlock : MachineBlock(defaultMachineSettings().nonOpaque()) {

    override fun getCasingDrop() = asItem()

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        IronFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(world, type, IronFurnaceBlockEntity::class.type()) { world1, pos, state, be -> be.tick(world1, pos, state) }

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
            val ironPlate = ic2_120.content.item.IronPlate::class.instance()
            if (ironPlate != Items.AIR) {
                ShapedRecipeBuilder.create(RecipeCategory.MISC, IronFurnaceBlock::class.item(), 1)
                    .pattern(" I ").pattern("IFI").pattern("I I")
                    .input('I', ironPlate).input('F', Items.FURNACE)
                    .criterion(hasItem(ironPlate), conditionsFromItem(ironPlate))
                    .offerTo(exporter, IronFurnaceBlock::class.id())
            }
        }
    }
}

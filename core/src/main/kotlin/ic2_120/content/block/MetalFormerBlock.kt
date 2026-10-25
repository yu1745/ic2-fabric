package ic2_120.content.block

import ic2_120.compat.save

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.MetalFormerBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.Coil
import ic2_120.content.item.ToolBox
import ic2_120.content.recipes.metalformer.MetalFormerRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
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
import ic2_120.registry.annotation.RecipeProvider

/**
 * 金属成型机方块。消耗电力将锭等压制成型（如锭 -> 粒/块）。
 * 能量等级：1
 */
@ModBlock(name = "metal_former", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class MetalFormerBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        MetalFormerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, MetalFormerBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            getMenuProvider(state, world, pos)?.let { factory ->
                player.openMenu(factory)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val coil = Coil::class.instance()
            val toolBox = ToolBox::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && coil != Items.AIR && toolBox != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, MetalFormerBlock::class.item(), 1)
                    .pattern(" c ")
                    .pattern("TMT")
                    .pattern("KKK")
                    .define('c', circuit)
                    .define('T', toolBox)
                    .define('M', machine)
                    .define('K', coil)
                    .unlockedBy(getHasName(machine), has(machine))
                    .save(exporter, MetalFormerBlock::class.id())
            }
            MetalFormerRecipeDatagen.generateRecipes(exporter)
        }
    }
}

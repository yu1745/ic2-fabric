package ic2_120.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.SolarGeneratorBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.CoalDust
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
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
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.function.Consumer

/**
 * 太阳能发电机方块。
 * 仅在白天、主世界、无遮挡、无雨雪时发电，1 EU/t 输出。
 */
@ModBlock(name = "solar_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class SolarGeneratorBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        SolarGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, SolarGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

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
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val generator = GeneratorBlock::class.item()
            val coalDust = CoalDust::class.instance()
            val circuit = Circuit::class.instance()
            if (generator != Items.AIR && coalDust != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SolarGeneratorBlock::class.item(), 1)
                    .pattern("DLD")
                    .pattern("LDL")
                    .pattern("CGC")
                    .define('D', coalDust)
                    .define('L', Items.GLASS)
                    .define('C', circuit)
                    .define('G', generator)
                    .unlockedBy(getHasName(generator), has(generator))
                    .save(exporter, SolarGeneratorBlock::class.id())
            }
        }
    }
}

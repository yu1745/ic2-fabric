package ic2_120.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient


import ic2_120.Ic2_120
import ic2_120.content.block.machines.CompressorBlockEntity
import ic2_120.content.recipes.compressor.CompressorRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
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
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 压缩机方块。消耗电力将物品压缩。
 * 能量等级：1
 */
@ModBlock(name = "compressor", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class CompressorBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        CompressorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, CompressorBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

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
            // 机器本身合成配方
            val machine = MachineCasingBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CompressorBlock::class.item(), 1)
                    .pattern("S S").pattern("SMS").pattern("SCS")
                    .define('S', Items.STONE).define('M', machine).define('C', circuit)
                    .unlockedBy(getHasName(machine), has(machine))
                    .save(exporter, CompressorBlock::class.id())
            }

            // 机器配方
            CompressorRecipeDatagen.generateRecipes(exporter)
        }
    }
}

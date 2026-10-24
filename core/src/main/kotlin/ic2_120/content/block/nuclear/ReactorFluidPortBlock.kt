package ic2_120.content.block.nuclear

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.item.EmptyCell
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory

/**
 * 反应堆流体接口。
 * 提供流体交互功能，通过管道输入冷却液/输出热冷却液。
 */
@ModBlock(name = "reactor_fluid_port", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor", renderLayer = "translucent")
class ReactorFluidPortBlock(settings: BlockBehaviour.Properties = ReactorShellBlock.shellSettings()) : ReactorShellBlock(settings) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ReactorFluidPortBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, ReactorFluidPortBlockEntity::class.type()) { w, p, s, be ->
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

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val vessel = ReactorVesselBlock::class.item()
            val emptyCell = EmptyCell::class.instance()
            if (vessel != Items.AIR && emptyCell != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ReactorFluidPortBlock::class.item(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .define('A', vessel)
                    .define('B', emptyCell)
                    .unlockedBy(getHasName(vessel), has(vessel))
                    .save(exporter, ReactorFluidPortBlock::class.id())
            }
        }
    }
}

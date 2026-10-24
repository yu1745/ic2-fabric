package ic2_120.content.block.nuclear

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer

/**
 * 反应堆红石接口。
 * 提供红石控制功能
 */
@ModBlock(name = "reactor_redstone_port", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor", renderLayer = "translucent")
class ReactorRedstonePortBlock(settings: BlockBehaviour.Properties = ReactorShellBlock.shellSettings()) : ReactorShellBlock(settings) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ReactorRedstonePortBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, ReactorRedstonePortBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val vessel = ReactorVesselBlock::class.item()
            if (vessel != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ReactorRedstonePortBlock::class.item(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .define('A', vessel)
                    .define('B', Items.REDSTONE)
                    .unlockedBy(getHasName(vessel), has(vessel))
                    .save(exporter, ReactorRedstonePortBlock::class.id())
            }
        }
    }
}

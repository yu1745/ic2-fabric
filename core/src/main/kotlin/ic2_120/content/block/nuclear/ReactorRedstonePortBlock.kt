package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory

/**
 * 反应堆红石接口。
 * 提供红石控制功能
 */
@ModBlock(name = "reactor_redstone_port", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorRedstonePortBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ReactorRedstonePortBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, ReactorRedstonePortBlockEntity::class.type()){ w, p, s, be ->
            (be as ReactorRedstonePortBlockEntity).tick(w, p, s)
        }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val vessel = ReactorVesselBlock::class.item()
            if (vessel != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReactorRedstonePortBlock::class.item(), 1)
                    .pattern("AAA").pattern("ABA").pattern("AAA")
                    .input('A', vessel)
                    .input('B', Items.REDSTONE)
                    .criterion(hasItem(vessel), conditionsFromItem(vessel))
                    .offerTo(exporter, ReactorRedstonePortBlock::class.id())
            }
        }
    }
}

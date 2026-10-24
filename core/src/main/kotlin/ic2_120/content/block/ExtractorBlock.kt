package ic2_120.content.block

import ic2_120.compat.validateTicker

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.isClient


import ic2_120.content.block.machines.ExtractorBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.content.item.Treetap
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeDatagen
import ic2_120.content.recipes.extractor.ExtractorRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
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
import ic2_120.registry.id

/**
 * 提取机方块。消耗电力从粘性树脂、橡胶原木、橡胶树苗等提取橡胶。
 * 能量等级：1
 */
@ModBlock(name = "extractor", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class ExtractorBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ExtractorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, ExtractorBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

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
            // 机器本身合成配方
            val machine = MachineCasingBlock::class.item()
            val circuit = Circuit::class.instance()
            val treetap = Treetap::class.instance()
            if (machine != Items.AIR && circuit != Items.AIR && treetap != Items.AIR) {
                ConsumeTreetapShapedRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = ExtractorBlock::class.id(),
                    result = ExtractorBlock::class.item(),
                    pattern = listOf("   ", "TMT", "TCT"),
                    keys = mapOf('T' to treetap, 'M' to machine, 'C' to circuit)
                )
            }

            // 机器配方
            ExtractorRecipeDatagen.generateRecipes(exporter)
        }
    }
}

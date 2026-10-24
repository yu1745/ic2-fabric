package ic2_120.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient


import ic2_120.content.block.machines.InductionFurnaceBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.entity.player.Player
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ic2_120.registry.id
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 感应炉方块。使用电力加热并快速烧制物品，支持双槽同时加工。
 * 热量机制：持续红石信号时热量上升（消耗 1 EU/t），无信号时热量衰减。
 * 能量等级：2（MV, 最高 128 EU/t）
 */
@ModBlock(name = "induction_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class InductionFurnaceBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        InductionFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, InductionFurnaceBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

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

    override fun getStateForPlacement(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val electricFurnace = ElectricFurnaceBlock::class.item()
            val advCasing = AdvancedMachineCasingBlock::class.item()
            if (electricFurnace != Items.AIR && advCasing != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InductionFurnaceBlock::class.item(), 1)
                    .pattern("CCC")
                    .pattern("CEC")
                    .pattern("CAC")
                    .define('C', Items.COPPER_INGOT)
                    .define('E', electricFurnace)
                    .define('A', advCasing)
                    .unlockedBy(getHasName(electricFurnace), has(electricFurnace))
                    .save(exporter, InductionFurnaceBlock::class.id())
            }
        }
    }
}

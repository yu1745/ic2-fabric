package ic2_120.content.block

import ic2_120.compat.save

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.has

import ic2_120.compat.isClient


import ic2_120.content.block.machines.ElectricFurnaceBlockEntity
import ic2_120.content.item.Circuit
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * 电炉方块。支持 facing（朝向）与 active（工作中）状态，与 ic2 行为一致。
 * 能量等级：1
 */
@ModBlock(name = "electric_furnace", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class ElectricFurnaceBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ElectricFurnaceBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, ElectricFurnaceBlockEntity::class.type()) { world1, pos, state, be -> be.tick(world1, pos, state) }

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
            val circuit = Circuit::class.instance()
            val ironFurnace = IronFurnaceBlock::class.item()
            if (circuit != Items.AIR && ironFurnace != Items.AIR) {
                ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ElectricFurnaceBlock::class.item(), 1)
                    .pattern("   ")
                    .pattern(" c ")
                    .pattern("RFR")
                    .define('c', circuit)
                    .define('R', Items.REDSTONE)
                    .define('F', ironFurnace)
                    .unlockedBy(getHasName(ironFurnace), has(ironFurnace))
                    .save(exporter, ElectricFurnaceBlock::class.id())
            }
        }
    }
}

package ic2_120.content.block

import ic2_120.compat.save

import ic2_120.compat.validateTicker

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.getHasName
import ic2_120.compat.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.item.ToolHandleIronItem
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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.Level
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer

@ModBlock(name = "wind_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class WindKineticGeneratorBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, WindKineticGeneratorBlock::class.item(), 1)
                .pattern("   ")
                .pattern("SCS")
                .pattern("   ")
                .define('S', ToolHandleIronItem::class.instance())
                .define('C', MachineCasingBlock::class.instance())
                .unlockedBy(
                    getHasName(MachineCasingBlock::class.instance()),
                    has(MachineCasingBlock::class.instance())
                )
                .save(exporter, WindKineticGeneratorBlock::class.id())
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        WindKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, WindKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        val be = world.getBlockEntity(pos) as? WindKineticGeneratorBlockEntity ?: return InteractionResult.PASS
        if (world.isClient) return InteractionResult.SUCCESS

        // TODO(1.21.1): useWithoutItem 不再携带 hand，保守取主手
        val interaction = be.useWithoutItem(player, InteractionHand.MAIN_HAND)
        if (interaction != InteractionResult.PASS) return interaction

        player.openMenu(be)
        return InteractionResult.SUCCESS
    }
}

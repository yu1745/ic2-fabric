package ic2_120.content.block

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient

import ic2_120.content.block.machines.CentrifugeBlockEntity
import ic2_120.content.item.Coil
import ic2_120.content.item.ElectricMotor
import ic2_120.content.item.MiningLaserItem
import ic2_120.content.recipes.centrifuge.CentrifugeRecipeDatagen
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
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

/**
 * 热能离心机方块。
 * 消耗电力与热量，将矿石、燃料棒等离心分离为多种产物。
 * 能量等级：2 (MV)
 */
@ModBlock(name = "centrifuge", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class CentrifugeBlock : MachineBlock(defaultMachineSettings() .noOcclusion()) {

    override fun getCasingDrop() = AdvancedMachineCasingBlock::class.item()

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        CentrifugeBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, CentrifugeBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

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

        /**
         * 为 ClassScanner 生成配方
         */
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val advCasing = AdvancedMachineCasingBlock::class.item()
            val coil = Coil::class.instance()
            val laser = MiningLaserItem::class.instance()
            val motor = ElectricMotor::class.instance()
            if (advCasing != Items.AIR && coil != Items.AIR && laser != Items.AIR && motor != Items.AIR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CentrifugeBlock::class.item(), 1)
                    .pattern("KLK")
                    .pattern("IAI")
                    .pattern("IEI")
                    .define('K', coil)
                    .define('L', laser)
                    .define('I', Items.IRON_INGOT)
                    .define('A', advCasing)
                    .define('E', motor)
                    .unlockedBy(getHasName(advCasing), has(advCasing))
                    .save(exporter, CentrifugeBlock::class.id())
            }
            CentrifugeRecipeDatagen.generateRecipes(exporter)
        }
    }
}

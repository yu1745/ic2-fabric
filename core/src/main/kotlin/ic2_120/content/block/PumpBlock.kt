package ic2_120.content.block

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.isClient

import ic2_120.Ic2_120
import ic2_120.content.block.MachineCasingBlock
import ic2_120.content.block.machines.PumpBlockEntity
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeDatagen
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil
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

@ModBlock(name = "pump", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "processing")
// 3D 模型不填满整格：nonOpaque 关闭对相邻方块的面剔除，避免紧贴方块被剔面而透视；碰撞箱仍为整格
class PumpBlock : DirectionalMachineBlock(MachineBlock.defaultMachineSettings() .noOcclusion()) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PumpBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, PumpBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, false)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            val storage = FluidStorage.SIDED.find(world, pos, hit.side)
                ?: FluidStorage.SIDED.find(world, pos, null)
            if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) {
                return InteractionResult.SUCCESS
            }
            getMenuProvider(state, world, pos)?.let(player::openHandledScreen)
        }
        return InteractionResult.SUCCESS
    }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val machine = MachineCasingBlock::class.item()
            val circuit = ic2_120.content.item.Circuit::class.instance()
            val emptyCell = ic2_120.content.item.EmptyCell::class.instance()
            val miningPipe = MiningPipeBlock::class.item()
            val treetap = ic2_120.content.item.Treetap::class.instance()
            if (machine != Items.AIR && emptyCell != Items.AIR && miningPipe != Items.AIR && treetap != Items.AIR && circuit != Items.AIR) {
                ConsumeTreetapShapedRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = PumpBlock::class.id(),
                    result = PumpBlock::class.item(),
                    pattern = listOf("ECE", "EME", "PTP"),
                    keys = mapOf('E' to emptyCell, 'C' to circuit, 'M' to machine, 'P' to miningPipe, 'T' to treetap)
                )
            }
        }
    }
}

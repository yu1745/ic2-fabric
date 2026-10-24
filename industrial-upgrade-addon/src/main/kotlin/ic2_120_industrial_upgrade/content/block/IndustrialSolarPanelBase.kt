package ic2_120_industrial_upgrade.content.block

import ic2_120.content.block.MachineBlock
import ic2_120_advanced_solar_addon.content.block.SolarPanelBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.MenuProvider
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/** 所有面板共用的 ACTIVE 属性，名称与 advanced-solar-addon 保持一致。 */
val SOLAR_ACTIVE: BooleanProperty = BooleanProperty.create("active")

/**
 * 工业升级附属所有太阳能发电机的抽象方块基类。
 * 交互/放置/状态属性逻辑统一，行为完全复用 ASA 的 SolarPanelBlockEntity。
 */
abstract class IndustrialSolarPanelBlock : MachineBlock() {
    override fun createScreenHandlerFactory(state: BlockState, world: Level, pos: BlockPos): MenuProvider? =
        world.getBlockEntity(pos) as? MenuProvider

    @Deprecated("Override without InteractionHand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) getMenuProvider(state, world, pos)?.let { player.openMenu(it) }
        return InteractionResult.SUCCESS
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(SOLAR_ACTIVE)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(SOLAR_ACTIVE, false)
}

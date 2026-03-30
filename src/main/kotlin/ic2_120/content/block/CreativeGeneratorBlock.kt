package ic2_120.content.block

import ic2_120.content.block.machines.CreativeGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 创造模式发电机方块。无限生成 32 EU/t，支持电池充电。
 *
 * - 与铁机不同的方块设置：不 [AbstractBlock.Settings.requiresTool]，便于空手挖掘；爆炸抗性同基岩，避免被炸毁。
 * - 标签上排除 `needs_iron_tool` / 强制镐类，避免空手挖矿倍率过低。
 * - 掉落物：见 [CreativeGeneratorBlockItem]（防火）与 [ic2_120.content.CreativeGeneratorItemEntityHandler]（永不清除、环境不伤实体）。
 */
@ModBlock(name = "creative_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class CreativeGeneratorBlock : MachineBlock(
    AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
        .strength(2.0f, 3600000.0f)
        .sounds(BlockSoundGroup.METAL)
) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CreativeGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, CreativeGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as CreativeGeneratorBlockEntity).tick(w, p, s)
        }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, true)

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): net.minecraft.screen.NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? net.minecraft.screen.NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    /**
     * 注册器扫描的 BlockItem；忽略传入的 [Item.Settings]，始终使用防火（岩浆中不烧毁）。
     */
    class CreativeGeneratorBlockItem(
        block: net.minecraft.block.Block,
        @Suppress("UNUSED_PARAMETER") settings: Item.Settings
    ) : BlockItem(block, FabricItemSettings().fireproof())

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

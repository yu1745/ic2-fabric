package ic2_120.content.block

import net.minecraft.world.level.block.entity.BlockEntityType

import ic2_120.compat.isClient

import ic2_120.content.block.machines.CreativeGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.SoundType
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * 创造模式发电机方块。无缓存，直接输出 512 EU/t。
 *
 * - 与铁机不同的方块设置：不 [BlockBehaviour.Properties.requiresTool]，便于空手挖掘；爆炸抗性同基岩，避免被炸毁。
 * - 标签上排除 `needs_iron_tool` / 强制镐类，避免空手挖矿倍率过低。
 * - 掉落物：见 [CreativeGeneratorBlockItem]（防火）与 [ic2_120.content.CreativeGeneratorItemEntityHandler]（永不清除、环境不伤实体）。
 */
@ModBlock(name = "creative_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class CreativeGeneratorBlock : MachineBlock(
    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
        .strength(2.0f, 3600000.0f)
        .sounds(SoundType.METAL)
         .noOcclusion() // 3D 模型不填满整格：关闭对相邻方块的面剔除
) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CreativeGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, CreativeGeneratorBlockEntity::class.type()) { w, p, s, be ->
            be.tick(w, p, s)
        }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(ctx)?.setValue(ACTIVE, true)

    /**
     * 注册器扫描的 BlockItem；忽略传入的 [Item.Properties]，始终使用防火（岩浆中不烧毁）。
     */
    class CreativeGeneratorBlockItem(
        block: net.minecraft.world.level.block.Block,
        @Suppress("UNUSED_PARAMETER") settings: Item.Properties
    ) : BlockItem(block, Item.Properties().fireproof())

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")
    }
}

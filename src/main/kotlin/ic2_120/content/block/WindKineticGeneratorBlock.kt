package ic2_120.content.block

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlock(name = "wind_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class WindKineticGeneratorBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        WindKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ModBlockEntities.getType(WindKineticGeneratorBlockEntity::class)) { _, _, _, _ ->
            // 当前版本不需要服务端逻辑；旋转由客户端渲染按世界时间驱动。
        }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        val be = world.getBlockEntity(pos) as? WindKineticGeneratorBlockEntity ?: return ActionResult.PASS
        if (world.isClient) return ActionResult.SUCCESS

        val interaction = be.onUse(player, hand)
        if (interaction != ActionResult.PASS) return interaction

        player.openHandledScreen(be)
        return ActionResult.SUCCESS
    }
}

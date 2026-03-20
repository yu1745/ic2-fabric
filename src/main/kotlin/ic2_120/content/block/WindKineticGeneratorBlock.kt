package ic2_120.content.block

import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
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
        else checkType(type, WindKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as WindKineticGeneratorBlockEntity).tick(w, p, s)
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

package ic2_120.client.renderers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.block.machines.BaseMinerBlockEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt

/** Displays the miner drill at its active target using 26.2 item render states. */
class MinerBlockEntityRenderer<T : BaseMinerBlockEntity>(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<T, MinerBlockEntityRenderer.State> {
    class State : BlockEntityRenderState() {
        var drill: ItemStackRenderState? = null
        var dx = 0.0
        var dy = 0.0
        var dz = 0.0
        var yaw = 0f
        var pitch = 0f
    }

    private val itemModelResolver = context.itemModelResolver()
    override fun createRenderState() = State()

    override fun extractRenderState(
        entity: T, state: State, partialTicks: Float, cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress)
        state.drill = null
        val drill = entity.getItem(BaseMinerBlockEntity.SLOT_DRILL)
        if (drill.isEmpty) return
        val world = entity.level ?: return
        val target = entity.getRenderDrillTarget(world.gameTime) ?: return
        val active = entity.blockState.getOptionalValue(BaseMinerBlock.ACTIVE).orElse(false)
        val vibration = if (active) (sin((world.gameTime + partialTicks) * 1.2f) * 0.06f).toDouble() else 0.0
        val x = target.x + 0.5
        val y = target.y + 0.5 + vibration
        val z = target.z + 0.5
        val vx = cameraPosition.x - x
        val vy = cameraPosition.y - y
        val vz = cameraPosition.z - z
        val horizontal = sqrt(vx * vx + vz * vz).coerceAtLeast(1.0E-6)
        state.yaw = Math.toDegrees(atan2(vx, vz)).toFloat()
        state.pitch = Math.toDegrees(atan2(vy, horizontal)).toFloat()
        state.dx = x - entity.blockPos.x
        state.dy = y - entity.blockPos.y
        state.dz = z - entity.blockPos.z
        state.drill = ItemStackRenderState().also {
            itemModelResolver.updateForTopItem(it, drill, ItemDisplayContext.FIXED, world, null, BlockPos.asLong(target.x, target.y, target.z).toInt())
        }
    }

    override fun submit(state: State, poseStack: PoseStack, nodes: SubmitNodeCollector, camera: CameraRenderState) {
        val drill = state.drill ?: return
        poseStack.pushPose()
        poseStack.translate(state.dx, state.dy, state.dz)
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw))
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch))
        poseStack.mulPose(Axis.YP.rotationDegrees(180f))
        poseStack.scale(0.85f, 0.85f, 0.85f)
        drill.submit(poseStack, nodes, state.lightCoords, 0, 0)
        poseStack.popPose()
    }

    override fun shouldRenderOffScreen(): Boolean = true
}

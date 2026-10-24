package ic2_120.client.renderers

import ic2_120.compat.cachedState
import ic2_120.compat.world

import ic2_120.content.block.BaseMinerBlock
import ic2_120.content.block.machines.BaseMinerBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.item.ItemDisplayContext
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt

class MinerBlockEntityRenderer<T : BaseMinerBlockEntity>(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<T> {

    private val itemRenderer = context.itemRenderer

    override fun render(
        entity: T,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val drill = entity.getItem(BaseMinerBlockEntity.SLOT_DRILL)
        if (drill.isEmpty) return

        val world = entity.level() ?: return
        val targetPos = entity.getRenderDrillTarget(world.gameTime) ?: return
        val active = entity.cachedState.getOptionalValue(BaseMinerBlock.ACTIVE).orElse(false)
        val vibration = if (active) (sin((world.gameTime + tickDelta) * 1.2f) * 0.06f).toDouble() else 0.0
        val renderX = targetPos.x + 0.5
        val renderY = targetPos.y + 0.5 + vibration
        val renderZ = targetPos.z + 0.5

        val camera = Minecraft.getInstance().gameRenderer.camera
        val cx = camera.pos.x
        val cy = camera.pos.y
        val cz = camera.pos.z
        val vx = cx - renderX
        val vy = cy - renderY
        val vz = cz - renderZ
        val horizontal = sqrt(vx * vx + vz * vz).coerceAtLeast(1.0E-6)
        val yawDeg = Math.toDegrees(atan2(vx, vz)).toFloat()
        val pitchDeg = Math.toDegrees(atan2(vy, horizontal)).toFloat()

        matrices.pushPose()
        // 在“当前被挖掘目标方块”中心附近渲染钻头。
        val dx = renderX - entity.pos.x
        val dy = renderY - entity.pos.y
        val dz = renderZ - entity.pos.z
        matrices.translate(dx, dy, dz)
        // Billboard：始终朝向玩家视角，避免 2D 贴图在侧视角不可见。
        matrices.mulPose(Axis.YP.rotationDegrees(yawDeg))
        matrices.mulPose(Axis.XP.rotationDegrees(-pitchDeg))
        matrices.mulPose(Axis.YP.rotationDegrees(180.0f))
        matrices.scale(0.85f, 0.85f, 0.85f)

        itemRenderer.renderItem(
            drill,
            ItemDisplayContext.FIXED,
            light,
            overlay,
            matrices,
            vertexConsumers,
            world,
            0
        )
        matrices.popPose()
    }

    override fun rendersOutsideBoundingBox(blockEntity: T): Boolean = true
}

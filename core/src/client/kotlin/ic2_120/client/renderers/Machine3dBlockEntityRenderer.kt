package ic2_120.client.renderers

import ic2_120.compat.cachedState
import ic2_120.compat.world

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ic2_120.Ic2_120
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import com.mojang.math.Axis
import net.minecraft.util.RandomSource
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.WeakHashMap
import kotlin.math.cos

/**
 * 3D 机器运动件的通用渲染器。
 *
 * 运动件几何为 OBJ 模型 `block/machine3d/<机器>_rotor_<i>[_active]`，动画参数来自
 * `assets/ic2_120/machine3d/animations.json`（由 tools/model3d/models.py 导出，枢轴只定义一次）：
 * - rotate：绕 axis 以 speed 度/tick 旋转，枢轴 pivot（模型单位）
 * - slide：沿 axis 往复平移，振幅 amplitude（模型单位），周期 period tick
 *
 * 机器以方块的 `active` 属性决定是否运动；停机时保持最后相位，避免跳回初始位置。
 */
class Machine3dBlockEntityRenderer<T : BlockEntity>(
    @Suppress("UNUSED_PARAMETER") context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<T> {

    class Part(val index: Int, val rotate: Boolean, val axis: Vector3f, val pivot: Vector3f, val speed: Float, val amplitude: Float, val period: Float)

    companion object {
        private val FACES = arrayOf(*Direction.values(), null)
        private val ANIMATIONS_ID = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "machine3d/animations.json")

        @Volatile
        private var animations: Map<String, List<Part>> = emptyMap()

        /** 资源重载时调用（与 OBJ 模型同一次加载）。 */
        fun reload(manager: ResourceManager) {
            val res = manager.getResource(ANIMATIONS_ID).orElse(null) ?: run { animations = emptyMap(); return }
            val root = res.open().bufferedReader().use { com.google.gson.JsonParser.parseReader(it).asJsonObject }
            animations = root.entrySet().associate { entry ->
                val (machine, list) = entry.key to entry.value
                machine to list.asJsonArray.map { parsePart(it.asJsonObject) }
            }
        }

        private fun parsePart(o: JsonObject): Part {
            fun vec(key: String) = o.getAsJsonArray(key)?.let { a -> Vector3f(a[0].asFloat, a[1].asFloat, a[2].asFloat) } ?: Vector3f()
            return Part(
                index = o.get("part").asInt,
                rotate = o.get("kind").asString == "rotate",
                axis = vec("axis"),
                pivot = vec("pivot").div(16f),
                speed = o.get("speed")?.asFloat ?: 0f,
                amplitude = (o.get("amplitude")?.asFloat ?: 0f) / 16f,
                period = o.get("period")?.asFloat ?: 20f,
            )
        }
    }

    private val frozenTime = WeakHashMap<BlockEntity, Float>()
    private val random = RandomSource.create()
    private val lights = IntArray(4)
    private val brightness = FloatArray(4)
    private val normal = Vector3f()

    /**
     * 原版 BlockModelRenderer 的矩阵渲染路径不做面朝向明暗，运动件会整体发白；
     * 这里按"变换后的法线"套用 MC 固定面亮度（上 1.0 / 南北 0.8 / 东西 0.6 / 下 0.5）。
     */
    private fun emitShaded(entry: PoseStack.Pose, consumer: VertexConsumer, model: BakedModel, light: Int) {
        lights.fill(light)
        for (face in FACES) {
            for (quad in model.getQuads(null, face, random)) {
                val n = quad.direction.normal
                normal.set(n.x, n.y, n.z).mul(entry.normal())
                val shade = normal.x * normal.x * 0.6f + normal.z * normal.z * 0.8f +
                    normal.y * normal.y * (if (normal.y > 0) 1f else 0.5f)
                brightness.fill(shade)
                consumer.putBulkData(entry, quad, brightness, 1.0f, 1.0f, 1.0f, 1.0f, lights, OverlayTexture.NO_OVERLAY, true)
            }
        }
    }

    override fun render(entity: T, tickDelta: Float, matrices: PoseStack, vertexConsumers: MultiBufferSource, light: Int, overlay: Int) {
        val world = entity.world ?: return
        val state = entity.cachedState
        val machine = BuiltInRegistries.BLOCK.getKey(state.block).path
        val parts = animations[machine] ?: return
        val activeProp = state.block.stateDefinition.getProperty("active") as? BooleanProperty
        val active = activeProp?.let { state.getValue(it) } ?: false
        val facing = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)
        // 动画时间：运行时随世界时间推进，停机时冻结
        val t = if (active) ((world.gameTime % 72000L) + tickDelta).also { frozenTime[entity] = it } else frozenTime[entity] ?: 0f

        val client = Minecraft.getInstance()
        val manager = client.modelManager
        val consumer = vertexConsumers.getBuffer(RenderType.solid())
        // 运动件从正面露出：取正前方方块的光照（机器自身格子光照会低一级）
        val frontLight = LevelRenderer.getLightColor(world, entity.pos.relative(facing))

        matrices.pushPose()
        // 与 blockstate 的 y 旋转一致：北为 0°，顺时针
        matrices.translate(0.5, 0.0, 0.5)
        matrices.mulPose(Axis.YN.rotationDegrees(facing.asRotation() - 180f))
        matrices.translate(-0.5, 0.0, -0.5)
        for (part in parts) {
            matrices.pushPose()
            if (part.rotate) {
                val angle = Math.toRadians(((t * part.speed) % 360f).toDouble()).toFloat()
                matrices.translate(part.pivot.x, part.pivot.y, part.pivot.z)
                matrices.mulPose(Quaternionf().rotationAxis(angle, part.axis.x, part.axis.y, part.axis.z))
                matrices.translate(-part.pivot.x, -part.pivot.y, -part.pivot.z)
            } else {
                val k = part.amplitude * (0.5f - 0.5f * cos((2.0 * Math.PI * t / part.period).toFloat()))
                matrices.translate(part.axis.x * k, part.axis.y * k, part.axis.z * k)
            }
            val id = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "block/machine3d/${machine}_rotor_${part.index}" + if (active) "_active" else "")
            manager.getModel(id)?.let { emitShaded(matrices.last(), consumer, it, frontLight) }
            matrices.popPose()
        }
        matrices.popPose()
    }
}

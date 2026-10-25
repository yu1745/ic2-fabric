package ic2_120.client.renderers

import com.google.gson.JsonObject
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import ic2_120.Ic2_120
import ic2_120.client.model.obj.ObjModelLoadingPlugin
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.WeakHashMap
import kotlin.math.cos

/** Animated OBJ machine parts rendered through 26.2's extraction and submission stages. */
class Machine3dBlockEntityRenderer<T : BlockEntity>(
    @Suppress("UNUSED_PARAMETER") context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<T, Machine3dBlockEntityRenderer.State> {
    class Part(val index: Int, val rotate: Boolean, val axis: Vector3f, val pivot: Vector3f, val speed: Float, val amplitude: Float, val period: Float)
    data class DrawPart(val animation: Part, val modelParts: List<BlockStateModelPart>)
    class State : BlockEntityRenderState() {
        var facing: Direction = Direction.NORTH
        var time = 0f
        var parts: List<DrawPart> = emptyList()
        var frontLight = 0
    }

    companion object {
        private val ANIMATIONS_ID = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "machine3d/animations.json")
        @Volatile private var animations: Map<String, List<Part>> = emptyMap()

        fun reload(manager: ResourceManager) {
            val resource = manager.getResource(ANIMATIONS_ID).orElse(null) ?: run { animations = emptyMap(); return }
            val root = resource.open().bufferedReader().use { com.google.gson.JsonParser.parseReader(it).asJsonObject }
            animations = root.entrySet().associate { entry ->
                entry.key to entry.value.asJsonArray.map { parsePart(it.asJsonObject) }
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

    override fun createRenderState() = State()

    override fun extractRenderState(
        entity: T, state: State, partialTicks: Float, cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress)
        val world = entity.level ?: run { state.parts = emptyList(); return }
        val blockState = entity.blockState
        val machine = BuiltInRegistries.BLOCK.getKey(blockState.block).path
        val animations = animations[machine] ?: run { state.parts = emptyList(); return }
        val activeProp = blockState.block.stateDefinition.getProperty("active") as? BooleanProperty
        val active = activeProp?.let { blockState.getValue(it) } ?: false
        state.facing = blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH)
        state.time = if (active) ((world.gameTime % 72000L) + partialTicks).also { frozenTime[entity] = it } else frozenTime[entity] ?: 0f
        state.frontLight = LightCoordsUtil.getLightCoords(world, entity.blockPos.relative(state.facing))
        val manager = Minecraft.getInstance().modelManager
        state.parts = animations.mapNotNull { part ->
            val id = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID,
                "block/machine3d/${machine}_rotor_${part.index}" + if (active) "_active" else "")
            val key = ObjModelLoadingPlugin.extraModelKey(id) ?: return@mapNotNull null
            val model = manager.getModel(key) ?: return@mapNotNull null
            val modelParts = ArrayList<BlockStateModelPart>()
            model.collectParts(random, modelParts)
            DrawPart(part, modelParts)
        }
    }

    override fun submit(state: State, poseStack: PoseStack, nodes: SubmitNodeCollector, camera: CameraRenderState) {
        if (state.parts.isEmpty()) return
        poseStack.pushPose()
        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YN.rotationDegrees(state.facing.toYRot() - 180f))
        poseStack.translate(-0.5, 0.0, -0.5)
        for ((part, modelParts) in state.parts) {
            poseStack.pushPose()
            if (part.rotate) {
                val angle = Math.toRadians(((state.time * part.speed) % 360f).toDouble()).toFloat()
                poseStack.translate(part.pivot.x, part.pivot.y, part.pivot.z)
                poseStack.mulPose(Quaternionf().rotationAxis(angle, part.axis.x, part.axis.y, part.axis.z))
                poseStack.translate(-part.pivot.x, -part.pivot.y, -part.pivot.z)
            } else {
                val k = part.amplitude * (0.5f - 0.5f * cos((2.0 * Math.PI * state.time / part.period).toFloat()))
                poseStack.translate(part.axis.x * k, part.axis.y * k, part.axis.z * k)
            }
            nodes.submitBlockModel(poseStack, RenderTypes.solidMovingBlock(), modelParts, intArrayOf(-1), state.frontLight, 0, 0)
            poseStack.popPose()
        }
        poseStack.popPose()
    }
}

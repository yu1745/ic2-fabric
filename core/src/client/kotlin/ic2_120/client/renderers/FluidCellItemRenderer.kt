package ic2_120.client.renderers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.serialization.MapCodec
import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.getFluidCellVariant
import ic2_120.client.compat.FluidRenderHandlerRegistry
import ic2_120.client.model.obj.ObjModelLoadingPlugin
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.client.renderer.special.SpecialModelRenderers
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.function.Consumer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 3D fluid cell renderer registered through the 26.2 special item-model codec. */
class FluidCellItemRenderer(private val slosh: Boolean) : SpecialModelRenderer<FluidCellItemRenderer.Contents> {
    companion object {
        val TYPE_ID: Identifier = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "fluid_cell")
        val SLOSH_TYPE_ID: Identifier = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "fluid_cell_slosh")
        private val SHELL = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "item/cell3d/shell")
        private val GLASS = Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "block/cell3d/glass")
        private const val SIDES = 8
        private const val PHASE = PI / 8
        private const val Y0 = 3.5f / 16
        private const val Y1 = 12.5f / 16
        private const val FLUID_R = 3.0f / 16
        private const val GLASS_R = 3.25f / 16
        private const val FILL = 0.7f
        private val STATIC_FILL = setOf("air_cell", "bio_cell")

        fun register() {
            SpecialModelRenderers.ID_MAPPER.put(TYPE_ID, Unbaked.MAP_CODEC)
            SpecialModelRenderers.ID_MAPPER.put(SLOSH_TYPE_ID, Unbaked.SLOSH_CODEC)
            ClientTickEvents.END_CLIENT_TICK.register { Slosh.tick(it) }
        }
    }

    data class Contents(val sprite: TextureAtlasSprite?, val color: Int, val glow: Boolean)
    private val random = RandomSource.create()

    class Unbaked(private val slosh: Boolean) : SpecialModelRenderer.Unbaked<Contents> {
        companion object {
            val MAP_CODEC: MapCodec<Unbaked> = MapCodec.unit(Unbaked(false))
            val SLOSH_CODEC: MapCodec<Unbaked> = MapCodec.unit(Unbaked(true))
        }
        override fun bake(context: SpecialModelRenderer.BakingContext): FluidCellItemRenderer = FluidCellItemRenderer(slosh)
        override fun type(): MapCodec<Unbaked> = if (slosh) SLOSH_CODEC else MAP_CODEC
    }

    override fun extractArgument(stack: ItemStack): Contents {
        val atlas = Minecraft.getInstance().atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)
        val name = BuiltInRegistries.ITEM.getKey(stack.item).path
        if (name in STATIC_FILL) return Contents(atlas.getSprite(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "block/cell3d/$name")), -1, false)
        val fluid: Fluid = when (val item = stack.item) {
            is ModFluidCell -> item.getFluid()
            else -> stack.getFluidCellVariant()?.fluid ?: return Contents(null, -1, false)
        }
        if (fluid == Fluids.EMPTY) return Contents(null, -1, false)
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return Contents(null, -1, false)
        val sprite = handler.getFluidSprites(null, null, fluid.defaultFluidState()).firstOrNull()
        val color = handler.getFluidColor(null, null, fluid.defaultFluidState())
        return Contents(sprite, color, fluid.defaultFluidState().createLegacyBlock().getLightEmission() > 0)
    }

    override fun getExtents(output: Consumer<Vector3fc>) {
        output.accept(Vector3f(0f, 0f, 0f))
        output.accept(Vector3f(1f, 1f, 1f))
    }

    override fun submit(
        contents: Contents?, matrices: PoseStack, nodes: SubmitNodeCollector,
        light: Int, overlay: Int, hasFoil: Boolean, outlineColor: Int
    ) {
        val manager = Minecraft.getInstance().modelManager
        val shellKey = ObjModelLoadingPlugin.extraModelKey(SHELL)
        if (shellKey != null) {
            val model = manager.getModel(shellKey)
            if (model != null) {
                val parts = ArrayList<BlockStateModelPart>()
                model.collectParts(random, parts)
                nodes.submitBlockModel(matrices, RenderTypes.solidMovingBlock(), parts, intArrayOf(-1), light, overlay, outlineColor)
            }
        }
        val c = contents
        if (c?.sprite != null) {
            val client = Minecraft.getInstance()
            val up = if (slosh) surfaceNormal(matrices.last(), client) else Vector3f(0f, 1f, 0f)
            val fluidLight = if (c.glow) LightCoordsUtil.FULL_BRIGHT else light
            nodes.submitCustomGeometry(matrices, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS)) { pose, consumer ->
                drawFluid(pose, consumer, c, up, fluidLight, overlay, if (slosh) Slosh.agitation else 0f)
            }
        }
        val glass = Minecraft.getInstance().atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(GLASS)
        nodes.submitCustomGeometry(matrices, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)) { pose, consumer ->
            drawGlass(pose, consumer, glass, light, overlay)
        }
    }

    private fun surfaceNormal(entry: PoseStack.Pose, client: Minecraft): Vector3f {
        if (client.level == null) return Vector3f(0f, 1f, 0f)
        val world = Slosh.normal(client.deltaTracker.getGameTimeDeltaPartialTick(false))
        val cam = client.gameRenderer.mainCamera()
        val view = Quaternionf().rotationX(cam.xRot() * Mth.DEG_TO_RAD).rotateY((cam.yRot() + 180f) * Mth.DEG_TO_RAD).transform(world)
        val local = Matrix3f(entry.normal()).invert().transform(view).normalize()
        if (local.y < 0.3f) local.y = 0.3f
        return local.normalize()
    }

    private fun ring(r: Float) = Array(SIDES) {
        val a = PHASE + 2 * PI * it / SIDES
        floatArrayOf(0.5f + r * cos(a).toFloat(), 0.5f + r * sin(a).toFloat())
    }

    private fun drawFluid(
        entry: PoseStack.Pose, vc: VertexConsumer, c: Contents, up: Vector3f, light: Int, overlay: Int, agitation: Float,
    ) {
        val pts = ring(FLUID_R)
        val center = Y0 + (Y1 - Y0) * FILL
        val t = (Minecraft.getInstance().level?.gameTime ?: 0L) + Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(false)
        // 每个角点的液面高度：过中心点、法线为 up 的平面，叠加随晃动强度衰减的小波纹
        val h = FloatArray(SIDES) {
            val dx = pts[it][0] - 0.5f
            val dz = pts[it][1] - 0.5f
            val ripple = agitation * Slosh.ripple * sin(t * 0.9f + it * 1.7f)
            (center - (up.x * dx + up.z * dz) / up.y + ripple).coerceIn(Y0 + 0.01f, Y1)
        }
        val r = (c.color shr 16 and 0xFF)
        val g = (c.color shr 8 and 0xFF)
        val b = (c.color and 0xFF)
        val sp = c.sprite ?: return
        fun v(x: Float, y: Float, z: Float, u: Float, vv: Float, nx: Float, ny: Float, nz: Float) {
            vc.addVertex(entry.pose(), x, y, z).setColor(r, g, b, 255)
                .setUv(sp.getU(u), sp.getV(vv))
                .setOverlay(overlay).setLight(light).setNormal(entry, nx, ny, nz)
        }
        for (k in 0 until SIDES) {
            val a = pts[k]
            val bb = pts[(k + 1) % SIDES]
            val nx = (a[0] + bb[0]) / 2 - 0.5f
            val nz = (a[1] + bb[1]) / 2 - 0.5f
            val u0 = k / SIDES.toFloat()
            val u1 = (k + 1) / SIDES.toFloat()
            // 侧面：v 按高度取，贴图在液柱上连续
            v(a[0], Y0, a[1], u0, 1f - Y0, nx, 0f, nz)
            v(a[0], h[k], a[1], u0, 1f - h[k], nx, 0f, nz)
            v(bb[0], h[(k + 1) % SIDES], bb[1], u1, 1f - h[(k + 1) % SIDES], nx, 0f, nz)
            v(bb[0], Y0, bb[1], u1, 1f - Y0, nx, 0f, nz)
        }
        // 液面：以中心为扇心的三角扇（四边形退化一个点）
        for (k in 0 until SIDES) {
            val a = pts[k]
            val bb = pts[(k + 1) % SIDES]
            v(0.5f, center, 0.5f, 0.5f, 0.5f, up.x, up.y, up.z)
            v(bb[0], h[(k + 1) % SIDES], bb[1], bb[0], bb[1], up.x, up.y, up.z)
            v(a[0], h[k], a[1], a[0], a[1], up.x, up.y, up.z)
            v(a[0], h[k], a[1], a[0], a[1], up.x, up.y, up.z)
        }
    }

    private fun drawGlass(entry: PoseStack.Pose, vc: VertexConsumer, sp: TextureAtlasSprite, light: Int, overlay: Int) {
        val pts = ring(GLASS_R)
        fun v(x: Float, y: Float, z: Float, u: Float, vv: Float, nx: Float, nz: Float) {
            vc.addVertex(entry.pose(), x, y, z).setColor(255, 255, 255, 64)
                .setUv(sp.getU(u), sp.getV(vv))
                .setOverlay(overlay).setLight(light).setNormal(entry, nx, 0f, nz)
        }
        for (k in 0 until SIDES) {
            val a = pts[k]
            val b = pts[(k + 1) % SIDES]
            val nx = (a[0] + b[0]) / 2 - 0.5f
            val nz = (a[1] + b[1]) / 2 - 0.5f
            val u0 = k / SIDES.toFloat()
            val u1 = (k + 1) / SIDES.toFloat()
            v(a[0], Y0, a[1], u0, 1f - Y0, nx, nz)
            v(a[0], Y1, a[1], u0, 1f - Y1, nx, nz)
            v(b[0], Y1, b[1], u1, 1f - Y1, nx, nz)
            v(b[0], Y0, b[1], u1, 1f - Y0, nx, nz)
        }
    }

    /**
     * 液面晃动：二维阻尼弹簧（世界 x/z 上的倾斜量）。
     * 驱动量为"手部估计位置"的二阶差分（含走动、转头带来的手部加速度）：
     * 等效重力 = g − a，液面法线朝加速度方向倾斜，随后弹簧回正并衰减。
     */
    object Slosh {
        // 参数来自 Ic2Config.rendering.fluidCell，ModMenu 配置界面保存后下一 tick 生效
        private val cfg get() = Ic2Config.current.rendering.fluidCell

        private var tx = 0f
        private var tz = 0f
        private var px = 0f
        private var pz = 0f
        private var vx = 0f
        private var vz = 0f
        private var lastHand: Vec3? = null
        private var lastVel: Vec3 = Vec3.ZERO
        var agitation = 0f
            private set
        val ripple get() = cfg.ripple.toFloat()

        fun tick(client: Minecraft) {
            val player = client.player ?: run { lastHand = null; return }
            val right = Vec3.directionFromRotation(0f, player.yRot + 90f)
            val hand = player.getEyePosition().add(player.getViewVector(1.0f).scale(0.6)).add(right.scale(0.35))
            // 传送/重生等瞬移不算运动
            val vel = lastHand?.let { hand.subtract(it) }?.takeIf { it.lengthSqr() < 4.0 } ?: Vec3.ZERO
            val acc = vel.subtract(lastVel)
            lastHand = hand
            lastVel = vel
            px = tx
            pz = tz
            val c = cfg
            val gain = if (c.slosh) c.gain.toFloat() else 0f
            val stiff = c.stiffness.toFloat()
            val keep = 1f - c.damping.toFloat().coerceIn(0f, 1f)
            vx = (vx + acc.x.toFloat() * gain - tx * stiff) * keep
            vz = (vz + acc.z.toFloat() * gain - tz * stiff) * keep
            tx += vx
            tz += vz
            val maxTilt = c.maxTilt.toFloat()
            val len = Mth.sqrt(tx * tx + tz * tz)
            if (len > maxTilt && len > 0f) {
                tx *= maxTilt / len
                tz *= maxTilt / len
            }
            agitation = (agitation * 0.9f + Mth.sqrt(vx * vx + vz * vz) * 3f).coerceAtMost(1f)
        }

        fun normal(delta: Float) =
            Vector3f(Mth.lerp(delta, px, tx), 1f, Mth.lerp(delta, pz, tz)).normalize()
    }
}

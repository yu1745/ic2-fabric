package ic2_120.client.renderers

import ic2_120.Ic2_120
import ic2_120.config.Ic2Config
import ic2_120.content.item.ModFluidCell
import ic2_120.content.item.getFluidCellVariant
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraft.util.RandomSource
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 流体单元 3D 物品渲染：
 * - 外壳（底座/钢箍/护条）为 OBJ 模型 `item/cell3d/shell`；
 * - 液柱用所含流体的真实贴图（与世界中流体渲染同源，水/熔岩等动画贴图照常播放），液面为倾斜平面；
 * - 玻璃为半透明八棱柱，罩在液柱外。
 *
 * 液面在世界坐标中保持水平；第一人称手持时由 [Slosh] 的阻尼弹簧根据手部加速度产生晃动。
 * 物品栏（GUI）中液面相对物品水平，不晃动。
 */
object FluidCellItemRenderer : BuiltinItemRendererRegistry.DynamicItemRenderer {

    private val SHELL = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "item/cell3d/shell")
    private val GLASS = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "item/cell3d/glass")

    private const val SIDES = 8
    private const val PHASE = PI / 8
    private const val Y0 = 3.5f / 16
    private const val Y1 = 12.5f / 16
    private const val FLUID_R = 3.0f / 16
    private const val GLASS_R = 3.25f / 16
    private const val FILL = 0.7f

    /** 无流体但有内容物的单元（压缩空气、生物质块）直接用生成贴图 */
    private val STATIC_FILL = setOf("air_cell", "bio_cell")

    private val CELLS = listOf(
        "fluid_cell", "empty_cell", "air_cell", "bio_cell", "water_cell", "lava_cell", "distilled_water_cell",
        "coolant_cell", "hot_coolant_cell", "uu_matter_cell", "weed_ex_cell", "pahoehoe_lava_cell",
        "biofuel_cell", "biomass_cell", "construction_foam_cell", "steam_cell", "superheated_steam_cell",
    )

    private val random = RandomSource.create()
    private val normal = Vector3f()

    fun register() {
        for (name in CELLS) {
            BuiltinItemRendererRegistry.INSTANCE.register(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, name)), this)
        }
        ClientTickEvents.END_CLIENT_TICK.register { Slosh.tick(it) }
    }

    private class Contents(val sprite: TextureAtlasSprite, val color: Int, val glow: Boolean)

    private fun contents(stack: ItemStack): Contents? {
        val atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS_TEXTURE)
        val name = BuiltInRegistries.ITEM.getId(stack.item).path
        if (name in STATIC_FILL) return Contents(atlas.apply(ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "item/cell3d/$name")), -1, false)
        val fluid: Fluid = when (val item = stack.item) {
            is ModFluidCell -> item.getFluid()
            else -> stack.getFluidCellVariant()?.fluid ?: return null
        }
        if (fluid == Fluids.EMPTY) return null
        val state = fluid.defaultFluidState().createLegacyBlock()
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return null
        val sprite = handler.getFluidSprites(null, null, state).firstOrNull() ?: return null
        val color = handler.getFluidColor(null, null, state)
        return Contents(sprite, color, state.createLegacyBlock().getLightEmission() > 0)
    }

    override fun render(
        stack: ItemStack,
        mode: ItemDisplayContext,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int,
    ) {
        val client = Minecraft.getInstance()
        val entry = matrices.last()

        val solid = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS_TEXTURE))
        val shell = client.modelManager.getModel(SHELL) ?: return
        for (face in FACES) {
            for (quad in shell.getQuads(null, face, random)) solid.putBulkData(entry, quad, 1f, 1f, 1f, light, overlay)
        }

        contents(stack)?.let { c ->
            val up = surfaceNormal(mode, entry, client)
            val fluidLight = if (c.glow) LightTexture.MAX_LIGHT_COORDINATE else light
            drawFluid(entry, solid, c, up, fluidLight, overlay, if (isFirstPerson(mode)) Slosh.agitation else 0f)
        }

        val glass = client.getTextureAtlas(InventoryMenu.BLOCK_ATLAS_TEXTURE).apply(GLASS)
        drawGlass(entry, vertexConsumers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS_TEXTURE)), glass, light, overlay)
    }

    private fun isFirstPerson(mode: ItemDisplayContext) =
        mode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND

    /**
     * 液面法线（物品局部坐标）。世界中：世界"上"（叠加晃动）→ 视图空间（相机旋转）→ 局部（法线矩阵逆）。
     * 渲染世界与第一人称手部时矩阵栈都在同一视图空间，故统一处理；GUI 直接取局部向上。
     */
    private fun surfaceNormal(mode: ItemDisplayContext, entry: PoseStack.Pose, client: Minecraft): Vector3f {
        if (mode == ItemDisplayContext.GUI || client.level == null) return Vector3f(0f, 1f, 0f)
        val world = if (isFirstPerson(mode)) Slosh.normal(client.frameTime) else Vector3f(0f, 1f, 0f)
        val cam = client.gameRenderer.camera
        val view = Quaternionf()
            .rotationX(cam.pitch * Mth.RADIANS_PER_DEGREE)
            .rotateY((cam.yaw + 180f) * Mth.RADIANS_PER_DEGREE)
            .transform(world)
        val local = Matrix3f(entry.normal()).invert().transform(view).normalize()
        // 倒置/横放时液面法线朝下会让液柱翻出，限制为至少略微朝上
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
        val t = (Minecraft.getInstance().level?.gameTime ?: 0L) + Minecraft.getInstance().tickDelta
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
        val sp = c.sprite
        fun v(x: Float, y: Float, z: Float, u: Float, vv: Float, nx: Float, ny: Float, nz: Float) {
            vc.vertex(entry.pose(), x, y, z).color(r, g, b, 255)
                .uv(sp.getU(u * 16.0f), sp.getV(vv * 16.0f))
                .overlay(overlay).light(light).normal(entry.normal(), nx, ny, nz).endVertex()
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
            vc.vertex(entry.pose(), x, y, z).color(255, 255, 255, 64)
                .uv(sp.getU(u * 16.0f), sp.getV(vv * 16.0f))
                .overlay(overlay).light(light).normal(entry.normal(), nx, 0f, nz).endVertex()
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

    private val FACES = arrayOf(*Direction.values(), null)

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
            val player = minecraft.player ?: run { lastHand = null; return }
            val right = Vec3.fromPolar(0f, player.yaw + 90f)
            val hand = player.eyePos.add(player.rotationVector.multiply(0.6)).add(right.multiply(0.35))
            // 传送/重生等瞬移不算运动
            val vel = lastHand?.let { hand.subtract(it) }?.takeIf { it.lengthSquared() < 4.0 } ?: Vec3.ZERO
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

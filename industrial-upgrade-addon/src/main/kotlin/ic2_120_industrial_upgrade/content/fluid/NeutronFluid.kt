package ic2_120_industrial_upgrade.content.fluid

import ic2_120.content.fluid.ModFluids
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.item.Item

/**
 * 中子流体（Neutron）：由中子制造机产出，桶装后经压缩机合成中子碎片。
 *
 * 复用 core 的 [ModFluids.registerFluidFor] 开放 API 注册，不再独立实现 Still/Flowing 子类。
 * 贴图走 core 的「通用纹理 + tint」模式（同 construction_foam/creosote/compressed_air）：
 * 不提供独立 PNG，客户端渲染统一引用 ic2:block/fluid/fluid_still / fluid_flow 并着色。
 */
object NeutronFluid {

    /** 中子流体（Still） */
    val NEUTRON_STILL: FlowingFluid by lazy {
        ModFluids.Ic2Fluid.stillFluidMap["neutron"] as? FlowingFluid
            ?: error("Neutron fluid not registered yet")
    }

    /** 中子流体（Flowing） */
    val NEUTRON_FLOWING: FlowingFluid by lazy {
        ModFluids.Ic2Fluid.flowingFluidMap["neutron"] as? FlowingFluid
            ?: error("Flowing neutron fluid not registered yet")
    }

    /** 中子流体方块 */
    lateinit var NEUTRON_BLOCK: Block
        private set

    /** 中子流体桶 */
    lateinit var NEUTRON_BUCKET: Item
        private set

    fun register() {
        val result = ModFluids.registerFluidFor(
            modId = IC2IndustrialUpgrade.MOD_ID,
            name = "neutron",
            stillTex = "fluid_still",
            flowTex = "fluid_flow",
            tintArgb = 0xFF8B7FD4.toInt() // 淡紫色，呼应中子主题（tint 同时供 GUI/JEI/单元着色查询）
        )
        NEUTRON_BLOCK = result.block
        NEUTRON_BUCKET = result.bucket
    }
}

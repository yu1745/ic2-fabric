package ic2_120_industrial_upgrade

import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.cables.CableProvider
import ic2_120.registry.ClassScanner
import ic2_120.registry.CreativeTab
import ic2_120_industrial_upgrade.content.block.DiffractiveCableBlock
import ic2_120_industrial_upgrade.content.block.InfinityCableBlock
import ic2_120_industrial_upgrade.content.block.MythicalCableBlock
import ic2_120_industrial_upgrade.content.block.NeutronCableBlock
import ic2_120_industrial_upgrade.content.block.PerfectCableBlock
import ic2_120_industrial_upgrade.content.block.PhotonicCableBlock
import ic2_120_industrial_upgrade.content.block.ProtonCableBlock
import ic2_120_industrial_upgrade.content.block.QuantumCableBlock
import ic2_120_industrial_upgrade.content.block.SingularCableBlock
import ic2_120_industrial_upgrade.content.block.SpectralCableBlock
import ic2_120_industrial_upgrade.content.block.UniversalCableBlock

/**
 * 向 core 贡献工业升级附属的 11 档高压玻璃 EU 导线。
 *
 * 通过 core 的 "ic2_120:cables" entrypoint 被 core 在 onInitialize 期间调用
 *（此时 core 尚未注册 CableBlockEntity，注册表也未冻结）。
 * 本 provider 实例化每档导线方块，调用 [ClassScanner.registerCableBlock] 以本附属 modId
 * 注册进 BuiltInRegistries（BLOCK + 方块物品 + 创造栏 + 渲染层），随后 core 扫描注册表时即可收编。
 */
class IUCableProvider : CableProvider {
    override fun registerCables(): List<BaseCableBlock> {
        val modId = IC2IndustrialUpgrade.MOD_ID
        return listOf(
            ClassScanner.registerCableBlock(modId, "spectral_cable", SpectralCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "proton_cable", ProtonCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "singular_cable", SingularCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "mythical_cable", MythicalCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "quantum_cable", QuantumCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "photonic_cable", PhotonicCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "neutron_cable", NeutronCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "perfect_cable", PerfectCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "universal_cable", UniversalCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "diffractive_cable", DiffractiveCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
            ClassScanner.registerCableBlock(modId, "infinity_cable", InfinityCableBlock(), CreativeTab.INDUSTRIAL_UPGRADE, "cable"),
        )
    }
}

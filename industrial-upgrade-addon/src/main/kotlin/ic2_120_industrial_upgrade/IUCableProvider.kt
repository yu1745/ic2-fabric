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
        fun register(name: String, factory: () -> BaseCableBlock): BaseCableBlock {
            val id = net.minecraft.resources.Identifier.fromNamespaceAndPath(modId, name)
            ic2_120.registry.BlockConstructionId26.CURRENT.set(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, id)
            )
            val block = try { factory() } finally { ic2_120.registry.BlockConstructionId26.CURRENT.remove() }
            return ClassScanner.registerCableBlock(modId, name, block, CreativeTab.INDUSTRIAL_UPGRADE, "cable")
        }
        return listOf(
            register("spectral_cable") { SpectralCableBlock() },
            register("proton_cable") { ProtonCableBlock() },
            register("singular_cable") { SingularCableBlock() },
            register("mythical_cable") { MythicalCableBlock() },
            register("quantum_cable") { QuantumCableBlock() },
            register("photonic_cable") { PhotonicCableBlock() },
            register("neutron_cable") { NeutronCableBlock() },
            register("perfect_cable") { PerfectCableBlock() },
            register("universal_cable") { UniversalCableBlock() },
            register("diffractive_cable") { DiffractiveCableBlock() },
            register("infinity_cable") { InfinityCableBlock() },
        )
    }
}
